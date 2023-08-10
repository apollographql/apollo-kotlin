import app.cash.turbine.test
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.network.ws.DefaultWebSocketEngine
import com.apollographql.apollo3.network.ws.KtorWebSocketEngine
import com.apollographql.apollo3.network.ws.WebSocketEngine
import com.apollographql.apollo3.testing.internal.runTest
import okio.ByteString.Companion.encode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WebSocketEngineTest {

  private fun textFrames(webSocketEngine: WebSocketEngine) = runTest {
    val webSocketServer = WebSocketServer()
    webSocketServer.start()
    webSocketServer.events.test {
      val connection = webSocketEngine.open(webSocketServer.url())
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketServer.WebSocketEvent.Connect)

      val sessionId = connectEvent.sessionId
      connection.send("client->server")
      val textFrame = awaitItem()
      assertTrue(textFrame is WebSocketServer.WebSocketEvent.TextFrame)
      assertEquals("client->server", textFrame.text)

      webSocketServer.sendText(sessionId, "server->client")
      val message = connection.receive()
      assertEquals("server->client", message)

      connection.close()
      val closeEvent = awaitItem()
      assertTrue(closeEvent is WebSocketServer.WebSocketEvent.Close)
      assertEquals(1000, closeEvent.reasonCode)

      cancelAndIgnoreRemainingEvents()
    }
    webSocketServer.stop()
  }

  @Test
  fun textFramesDefault() = textFrames(DefaultWebSocketEngine())

  @Test
  fun textFramesKtor() = textFrames(KtorWebSocketEngine())

  private fun binaryFrames(webSocketEngine: WebSocketEngine) = runTest {
    val webSocketServer = WebSocketServer()
    webSocketServer.start()
    webSocketServer.events.test {
      val connection = webSocketEngine.open(webSocketServer.url())
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketServer.WebSocketEvent.Connect)

      val sessionId = connectEvent.sessionId
      connection.send("client->server".encode())
      val binaryFrame = awaitItem()
      assertTrue(binaryFrame is WebSocketServer.WebSocketEvent.BinaryFrame)
      assertEquals("client->server", binaryFrame.bytes.decodeToString())

      webSocketServer.sendBinary(sessionId, "server->client".toByteArray())
      val message = connection.receive()
      assertEquals("server->client", message)

      connection.close()
      val closeEvent = awaitItem()
      assertTrue(closeEvent is WebSocketServer.WebSocketEvent.Close)
      assertEquals(1000, closeEvent.reasonCode)

      cancelAndIgnoreRemainingEvents()
    }
    webSocketServer.stop()
  }

  @Test
  fun binaryFramesDefault() = binaryFrames(DefaultWebSocketEngine())

  @Test
  fun binaryFramesKtor() = binaryFrames(KtorWebSocketEngine())

  private fun serverClose(webSocketEngine: WebSocketEngine) = runTest {
    val webSocketServer = WebSocketServer()
    webSocketServer.start()
    webSocketServer.events.test {
      val connection = webSocketEngine.open(webSocketServer.url())
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketServer.WebSocketEvent.Connect)

      val sessionId = connectEvent.sessionId
      webSocketServer.sendClose(sessionId, reasonCode = 4200, reasonMessage = "Bye now")

      val e = assertFailsWith<ApolloWebSocketClosedException> {
        connection.receive()
      }
      assertEquals(4200, e.code)
      assertEquals("Bye now", e.reason)

      cancelAndIgnoreRemainingEvents()
    }
    webSocketServer.stop()
  }

  @Test
  fun serverCloseDefault() = serverClose(DefaultWebSocketEngine())

  @Test
  fun serverCloseKtor() = serverClose(KtorWebSocketEngine())

  private fun headers(webSocketEngine: WebSocketEngine) = runTest {
    val webSocketServer = WebSocketServer()
    webSocketServer.start()
    webSocketServer.events.test {
      webSocketEngine.open(webSocketServer.url(), listOf(
          HttpHeader("Sec-WebSocket-Protocol", "graphql-ws"),
          HttpHeader("header1", "value1"),
          HttpHeader("header2", "value2"),
      ))
      val connectEvent = awaitItem()
      assertTrue(connectEvent is WebSocketServer.WebSocketEvent.Connect)
      assertEquals("graphql-ws", connectEvent.headers["Sec-WebSocket-Protocol"])
      assertEquals("value1", connectEvent.headers["header1"])
      assertEquals("value2", connectEvent.headers["header2"])

      cancelAndIgnoreRemainingEvents()
    }
    webSocketServer.stop()
  }

  @Test
  fun headersDefault() = headers(DefaultWebSocketEngine())

  @Test
  fun headersKtor() = headers(KtorWebSocketEngine())

}
