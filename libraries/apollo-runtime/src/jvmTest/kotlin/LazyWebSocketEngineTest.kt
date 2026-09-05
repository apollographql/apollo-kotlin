import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.network.websocket.LazyWebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocket
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketListener
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class LazyWebSocketEngineTest {
  @Test
  fun closingBeforeUseDoesNotCreateTheDelegate() {
    val engine = LazyWebSocketEngine { error("Must not initialize the delegate") }
    engine.close()
    engine.close()

    assertFailsWith<IllegalArgumentException> {
      engine.newWebSocket("wss://example.com", emptyList(), listener)
    }
  }

  @Test
  fun createsTheDelegateOnceAndClosesItOnce() {
    var creations = 0
    var closes = 0
    val headers = listOf(HttpHeader("Authorization", "token"))
    val engine = LazyWebSocketEngine {
      creations++
      object : WebSocketEngine {
        override fun newWebSocket(url: String, headers: List<HttpHeader>, listener: WebSocketListener): WebSocket {
          assertEquals("wss://example.com", url)
          assertEquals(listOf(HttpHeader("Authorization", "token")), headers)
          assertSame(this@LazyWebSocketEngineTest.listener, listener)
          return webSocket
        }

        override fun close() {
          closes++
        }
      }
    }

    assertEquals(0, creations)
    repeat(2) {
      assertSame(webSocket, engine.newWebSocket("wss://example.com", headers, listener))
    }
    assertEquals(1, creations)
    engine.close()
    engine.close()
    assertEquals(1, closes)
    assertFailsWith<IllegalArgumentException> {
      engine.newWebSocket("wss://example.com", headers, listener)
    }
  }

  private val listener = object : WebSocketListener {
    override fun onOpen() = Unit
    override fun onMessage(text: String) = Unit
    override fun onMessage(data: ByteArray) = Unit
    override fun onError(cause: ApolloException) = Unit
    override fun onClosed(code: Int?, reason: String?) = Unit
  }

  private val webSocket = object : WebSocket {
    override fun send(data: ByteArray) = Unit
    override fun send(text: String) = Unit
    override fun close(code: Int, reason: String) = Unit
  }
}
