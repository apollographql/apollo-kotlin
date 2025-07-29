package test

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.network.websocket.WebSocketEngine
import com.apollographql.apollo.network.websocket.WebSocketListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class EchoServerWebSocketEngineTest {
  @OptIn(ApolloExperimental::class)
  @Test
  fun test() = runTest {
    withContext(Dispatchers.Default.limitedParallelism(1)) {
      val channel = Channel<Any>(Channel.UNLIMITED)
      val webSocket = WebSocketEngine().use {
        it.newWebSocket("http://localhost:18923/echo", listener = object : WebSocketListener {
          override fun onOpen() {
            channel.trySend(Opened)
          }

          override fun onMessage(text: String) {
            channel.trySend(text)
          }

          override fun onMessage(data: ByteArray) {
            channel.trySend(data)
          }

          override fun onError(cause: ApolloException) {
            channel.trySend(cause)
          }

          override fun onClosed(code: Int?, reason: String?) {
            channel.trySend(code to reason)
            channel.close()
          }
        })
      }
      channel.receiveOrTimeout().apply {
        assertEquals(Opened, this)
      }
      webSocket.send("A")
      channel.receiveOrTimeout().apply {
        assertEquals("A", this)
      }
      webSocket.send("B".encodeToByteArray())
      channel.receiveOrTimeout().apply {
        assertIs<ByteArray>(this)
        assertEquals("B", this.decodeToString())
      }
      webSocket.send("C")
      channel.receiveOrTimeout().apply {
        assertEquals("C", this)
      }
      webSocket.send("bye")
      channel.receiveOrTimeout().apply {
        assertIs<Pair<*, *>>(this)
        assertEquals(4400, first)
        assertEquals("bye", second)
      }
    }
  }
}

private data object Opened
private suspend fun Channel<Any>.receiveOrTimeout(): Any = withTimeout(1000) { receive() }