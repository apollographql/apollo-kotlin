
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloWebSocketClosedException
import com.apollographql.apollo3.mockserver.CloseFrame
import com.apollographql.apollo3.mockserver.DataMessage
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.TextMessage
import com.apollographql.apollo3.mockserver.awaitWebSocketRequest
import com.apollographql.apollo3.mockserver.enqueueWebSocket
import com.apollographql.apollo3.mpp.Platform
import com.apollographql.apollo3.mpp.platform
import com.apollographql.apollo3.testing.internal.runTest
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertTrue

class WebSocketEngineTest {

  /**
   * NSURLSession has a bug that sometimes skips the close frame
   * See https://developer.apple.com/forums/thread/679446
   */
  private fun maySkipCloseFrame(): Boolean {
    return platform() == Platform.Native
  }

  @Test
  fun textFrames() = runTest {
    val webSocketEngine = webSocketEngine()
    val webSocketServer = MockServer()

    val responseBody = webSocketServer.enqueueWebSocket()

    val connection = webSocketEngine.open(webSocketServer.url())
    val request = webSocketServer.awaitWebSocketRequest()

    connection.send("client->server")
    request.awaitMessage().apply {
      assertIs<TextMessage>(this)
      assertEquals("client->server", text)
    }

    responseBody.enqueueMessage(TextMessage("server->client"))
    assertEquals("server->client", connection.receive())

    connection.close()
    if (!maySkipCloseFrame()) {
      request.awaitMessage().apply {
        assertIs<CloseFrame>(this)
      }
    }

    webSocketServer.close()
  }


  @Test
  fun binaryFrames() = runTest {
    if (platform() == Platform.Js) return@runTest // Binary frames are not supported by the JS WebSocketEngine

    val webSocketEngine = webSocketEngine()
    val webSocketServer = MockServer()

    val responseBody = webSocketServer.enqueueWebSocket()

    val connection = webSocketEngine.open(webSocketServer.url())
    val request = webSocketServer.awaitWebSocketRequest()

    connection.send("client->server".encodeUtf8())
    request.awaitMessage().apply {
      assertIs<DataMessage>(this)
      assertEquals("client->server", data.decodeToString())
    }

    responseBody.enqueueMessage(DataMessage("server->client".encodeToByteArray()))
    assertEquals("server->client", connection.receive())

    connection.close()
    if (!maySkipCloseFrame()) {
      request.awaitMessage().apply {
        assertIs<CloseFrame>(this)
      }
    }

    webSocketServer.close()
  }

  @Test
  fun serverCloseNicely() = runTest {
    if (platform() == Platform.Js) return@runTest // It's not clear how termination works on JS

    val webSocketEngine = webSocketEngine()
    val webSocketServer = MockServer()

    val responseBody = webSocketServer.enqueueWebSocket()
    val connection = webSocketEngine.open(webSocketServer.url())

    responseBody.enqueueMessage(CloseFrame(4200, "Bye now"))
    val e = assertFailsWith<ApolloException> {
      connection.receive()
    }

    // On Apple, the close code and reason are not available - https://youtrack.jetbrains.com/issue/KTOR-6198
    if (!isKtor || platform() != Platform.Native) {
      assertTrue(e is ApolloWebSocketClosedException)
      assertEquals(4200, e.code)
      assertEquals("Bye now", e.reason)
    }

    connection.close()
    webSocketServer.close()
  }

  @Test
  fun serverCloseAbruptly() = runTest {
    if (platform() == Platform.Js) return@runTest // It's not clear how termination works on JS
    if (platform() == Platform.Native) return@runTest // https://youtrack.jetbrains.com/issue/KTOR-6406
    
    val webSocketEngine = webSocketEngine()
    val webSocketServer = MockServer()

    webSocketServer.enqueueWebSocket()
    val connection = webSocketEngine.open(webSocketServer.url())

    webSocketServer.close()

    assertFailsWith<ApolloNetworkException> {
      connection.receive()
    }

    connection.close()
  }

  @Test
  fun headers() = runTest {
    val webSocketEngine = webSocketEngine()
    val webSocketServer = MockServer()

    webSocketServer.enqueueWebSocket()

      webSocketEngine.open(webSocketServer.url(), listOf(
          HttpHeader("Sec-WebSocket-Protocol", "graphql-ws"),
          HttpHeader("header1", "value1"),
          HttpHeader("header2", "value2"),
      ))

    val request = webSocketServer.awaitWebSocketRequest()

    assertEquals("graphql-ws", request.headers["Sec-WebSocket-Protocol"])
    assertEquals("value1", request.headers["header1"])
    assertEquals("value2", request.headers["header2"])

    webSocketServer.close()
  }
}
