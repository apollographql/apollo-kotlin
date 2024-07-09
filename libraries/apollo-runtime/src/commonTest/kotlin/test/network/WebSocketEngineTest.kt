package test.network

import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.network.websocket.WebSocket
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketListener
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.CloseFrame
import com.apollographql.mockserver.DataMessage
import com.apollographql.mockserver.MockRequestBase
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.PingFrame
import com.apollographql.mockserver.PongFrame
import com.apollographql.mockserver.TextMessage
import com.apollographql.mockserver.WebSocketBody
import com.apollographql.mockserver.WebSocketMessage
import com.apollographql.mockserver.WebsocketMockRequest
import com.apollographql.mockserver.awaitWebSocketRequest
import com.apollographql.mockserver.enqueueWebSocket
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import okio.ByteString.Companion.toByteString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

private data class Item(
    val message: WebSocketMessage? = null,
    val open: Boolean = false,
    val exception: ApolloException? = null,
)

private class Listener(private val channel: Channel<Item>) : WebSocketListener {
  override fun onOpen() {
    channel.trySend(Item(open = true))
  }

  override fun onMessage(text: String) {
    channel.trySend(TextMessage(text))
  }

  override fun onMessage(data: ByteArray) {
    channel.trySend(DataMessage(data))
  }

  override fun onError(cause: ApolloException) {
    channel.trySend(Item(exception = cause))
  }

  override fun onClosed(code: Int?, reason: String?) {
    channel.trySend(CloseFrame(code, reason))
  }
}

private fun debug(line: String) {
  @Suppress("ConstantConditionIf")
  if (false) {
    println(line)
  }
}

internal val mockServerListener = object : MockServer.Listener {
  override fun onRequest(request: MockRequestBase) {
    debug("Client: ${request.method} ${request.path}")
  }

  override fun onMessage(message: WebSocketMessage) {
    debug("Client: ${message.pretty()}")
  }
}

private fun WebSocketMessage.pretty(): String = when (this) {
  is TextMessage -> {
    "TextMessage(${this.text})"
  }

  is DataMessage -> {
    "BinaryMessage(${this.data.toByteString().hex()})"
  }

  is CloseFrame -> {
    "CloseFrame($code, $reason)"
  }

  PingFrame -> "PingFrame"
  PongFrame -> "PongFrame"
}

class WebSocketEngineTest {
  private class Scope(
      val clientReader: Channel<Item>,
      val clientWriter: WebSocket,
      val serverReader: WebsocketMockRequest,
      val serverWriter: WebSocketBody,
  )

  private fun whenHandshakeDone(block: suspend Scope.() -> Unit) = runTest {
    val mockServer = MockServer.Builder()
        .listener(mockServerListener)
        .build()

    val engine = WebSocketEngine()

    val clientReader = Channel<Item>(Channel.UNLIMITED)

    val serverWriter = mockServer.enqueueWebSocket()
    val clientWriter = engine.newWebSocket(mockServer.url(), emptyList(), Listener(clientReader))

    val serverReader = mockServer.awaitWebSocketRequest()

    clientReader.awaitOpen()

    Scope(clientReader, clientWriter, serverReader, serverWriter).block()

    mockServer.close()
    engine.close()
  }

  @Test
  fun simpleSessionWithClientClose() = whenHandshakeDone {
    clientWriter.send("Client Text")
    serverReader.awaitMessage().apply {
      assertIs<TextMessage>(this)
      assertEquals("Client Text", this.text)
    }

    serverWriter.enqueueMessage(TextMessage("Server Text"))
    clientReader.awaitMessage().apply {
      assertIs<TextMessage>(this)
      assertEquals("Server Text", this.text)
    }

    clientWriter.send("Client Data".encodeToByteArray())
    serverReader.awaitMessage().apply {
      assertIs<DataMessage>(this)
      assertEquals("Client Data", this.data.decodeToString())
    }

    serverWriter.enqueueMessage(DataMessage("Server Data".encodeToByteArray()))
    clientReader.awaitMessage().apply {
      assertIs<DataMessage>(this)
      assertEquals("Server Data", this.data.decodeToString())
    }

    clientWriter.close(1003, "Client Bye")
    @Suppress("DEPRECATION")
    if (com.apollographql.apollo.testing.platform() != com.apollographql.apollo.testing.Platform.Native) {
      // Apple sometimes does not send the Close frame. See https://developer.apple.com/forums/thread/679446
      serverReader.awaitMessage().apply {
        assertIs<CloseFrame>(this)
        assertEquals(1003, this.code)
        assertEquals("Client Bye", this.reason)
      }
    }
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  @Test
  fun serverClose() = whenHandshakeDone {
    serverWriter.enqueueMessage(CloseFrame(1002, "Server Bye"))

    val item = clientReader.awaitItem()

    if (item.message != null) {
      item.message.apply {
        assertIs<CloseFrame>(this)
        assertEquals(1002, code)
        assertEquals("Server Bye", reason)
      }
    } else if (item.exception != null) {
      // Apple implementation calls onError instead on onClose
    }

    assertTrue(clientReader.isEmpty)
  }
}

private suspend fun Channel<Item>.awaitItem(): Item = withTimeout(1.seconds) {
  receive()
}

private suspend fun Channel<Item>.awaitMessage(): WebSocketMessage = withTimeout(1.seconds) {
  val item = receive()
  check(item.message != null) {
    "Expected message item, received $item"
  }
  item.message
}

private suspend fun Channel<Item>.awaitOpen() = withTimeout(1.seconds) {
  val item = receive()
  check(item.open) {
    "Expected open item, received $item"
  }
}

private fun Channel<Item>.trySend(message: WebSocketMessage) {
  debug("Server: ${message.pretty()}")
  trySend(Item(message = message))
}
