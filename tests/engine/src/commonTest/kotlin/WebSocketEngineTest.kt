import app.cash.turbine.test
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.mockserver.WebSocketMockServer
import com.apollographql.apollo3.mockserver.WebSocketMockServer.WebSocketEvent
import com.apollographql.apollo3.mpp.Platform
import com.apollographql.apollo3.mpp.platform
import com.apollographql.apollo3.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo3.network.ws.KtorWebSocketEngine
import com.apollographql.apollo3.network.ws.WebSocketEngine
import com.apollographql.apollo3.testing.internal.runTest
import io.ktor.utils.io.core.toByteArray
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WebSocketEngineTest {

  private fun textFrames(webSocketEngine: WebSocketEngine) = runTest {
    if (platform() == Platform.Js) return@runTest // JS doesn't have a WebSocketMockServer yet
    val webSocketServer = WebSocketMockServer()
    webSocketServer.start()
    webSocketServer.events.test {
      val connection = webSocketEngine.open(webSocketServer.url())
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketEvent.Connect)

      val sessionId = connectEvent.sessionId
      connection.send("client->server")
      val textFrame = awaitItem()
      assertTrue(textFrame is WebSocketEvent.TextFrame)
      assertEquals("client->server", textFrame.text)

      webSocketServer.sendText(sessionId, "server->client")
      val message = connection.receive()
      assertEquals("server->client", message)

      connection.close()
      val closeEvent = awaitItem()
      assertTrue(closeEvent is WebSocketEvent.Close)

      cancelAndIgnoreRemainingEvents()
    }
    webSocketServer.close()
  }

  @Test
  fun textFramesDefault() = textFrames(DefaultWebSocketEngine())

  @Test
  fun textFramesKtor() = textFrames(KtorWebSocketEngine())

  private fun binaryFrames(webSocketEngine: WebSocketEngine) = runTest {
    if (platform() == Platform.Js) return@runTest // JS doesn't have a WebSocketMockServer yet
    val webSocketServer = WebSocketMockServer()
    webSocketServer.start()
    webSocketServer.events.test {
      val connection = webSocketEngine.open(webSocketServer.url())
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketEvent.Connect)

      val sessionId = connectEvent.sessionId
      connection.send("client->server".encodeUtf8())
      val binaryFrame = awaitItem()
      assertTrue(binaryFrame is WebSocketEvent.BinaryFrame)
      assertEquals("client->server", binaryFrame.bytes.decodeToString())

      webSocketServer.sendBinary(sessionId, "server->client".toByteArray())
      val message = connection.receive()
      assertEquals("server->client", message)

      connection.close()
      val closeEvent = awaitItem()
      assertTrue(closeEvent is WebSocketEvent.Close)

      cancelAndIgnoreRemainingEvents()
    }
    webSocketServer.close()
  }

  @Test
  fun binaryFramesDefault() = binaryFrames(DefaultWebSocketEngine())

  @Test
  fun binaryFramesKtor() = binaryFrames(KtorWebSocketEngine())

  private fun serverCloseNicely(webSocketEngine: WebSocketEngine, checkCodeAndReason: Boolean = true) = runTest {
    if (platform() == Platform.Js) return@runTest // JS doesn't have a WebSocketMockServer yet
    val webSocketServer = WebSocketMockServer()
    webSocketServer.start()
    webSocketServer.events.test {
      val connection = webSocketEngine.open(webSocketServer.url())
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketEvent.Connect)

      val sessionId = connectEvent.sessionId
      webSocketServer.sendClose(sessionId, reasonCode = 4200, reasonMessage = "Bye now")

      val e = assertFailsWith<ApolloException> {
        connection.receive()
      }
      if (checkCodeAndReason) {
        assertTrue(e is ApolloWebSocketClosedException)
        assertEquals(4200, e.code)
        assertEquals("Bye now", e.reason)
      }

      cancelAndIgnoreRemainingEvents()
    }
    webSocketServer.close()
  }

  @Test
  fun serverCloseNicelyDefault() = serverCloseNicely(DefaultWebSocketEngine())

  @Test
  fun serverCloseNicelyKtor() = serverCloseNicely(
      webSocketEngine = KtorWebSocketEngine(),

      // On Apple, the close code and reason are not available - https://youtrack.jetbrains.com/issue/KTOR-6198
      checkCodeAndReason = platform() != Platform.Native
  )

  private fun serverCloseAbruptly(webSocketEngine: WebSocketEngine) = runTest {
    if (platform() == Platform.Js) return@runTest // JS doesn't have a WebSocketMockServer yet
    val webSocketServer = WebSocketMockServer()
    webSocketServer.start()
    webSocketServer.events.test {
      val connection = webSocketEngine.open(webSocketServer.url())
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketEvent.Connect)

      webSocketServer.close()
      assertFailsWith<ApolloNetworkException> {
        connection.receive()
      }
      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun serverCloseAbruptlyDefault() = serverCloseAbruptly(DefaultWebSocketEngine())

  @Test
  fun serverCloseAbruptlyKtor() = serverCloseAbruptly(KtorWebSocketEngine())

  private fun headers(webSocketEngine: WebSocketEngine) = runTest {
    if (platform() == Platform.Js) return@runTest // JS doesn't have a WebSocketMockServer yet
    val webSocketServer = WebSocketMockServer()
    webSocketServer.start()
    webSocketServer.events.test {
      webSocketEngine.open(webSocketServer.url(), listOf(
          HttpHeader("Sec-WebSocket-Protocol", "graphql-ws"),
          HttpHeader("header1", "value1"),
          HttpHeader("header2", "value2"),
      ))
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketEvent.Connect)
      assertEquals("graphql-ws", connectEvent.headers["Sec-WebSocket-Protocol"])
      assertEquals("value1", connectEvent.headers["header1"])
      assertEquals("value2", connectEvent.headers["header2"])

      cancelAndIgnoreRemainingEvents()
    }
    webSocketServer.close()
  }

  @Test
  fun headersDefault() = headers(DefaultWebSocketEngine())

  @Test
  fun headersKtor() = headers(KtorWebSocketEngine())
}
