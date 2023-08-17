package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.flow.Flow

@ApolloExperimental
interface WebSocketMockServer {
  @ApolloExperimental
  sealed class WebSocketEvent {
    @ApolloExperimental
    class Connect(val sessionId: String, val headers: Map<String, String>) : WebSocketEvent()

    @ApolloExperimental
    class TextFrame(val sessionId: String, val text: String) : WebSocketEvent()

    @ApolloExperimental
    class BinaryFrame(val sessionId: String, val bytes: ByteArray) : WebSocketEvent()

    @ApolloExperimental
    class Close(val sessionId: String, val reasonCode: Short?, val reasonMessage: String?) : WebSocketEvent()

    @ApolloExperimental
    class Error(val sessionId: String, val cause: Throwable) : WebSocketEvent()
  }

  fun start()
  suspend fun url(): String

  val events: Flow<WebSocketEvent>

  suspend fun sendText(sessionId: String, text: String)
  suspend fun sendBinary(sessionId: String, binary: ByteArray)
  suspend fun sendClose(sessionId: String, reasonCode: Short? = null, reasonMessage: String? = null)
  fun stop()
}

@ApolloExperimental
/**
 * @param port the port to listen on. If 0, a random available port will be used.
 */
expect fun WebSocketMockServer(port: Int = 0): WebSocketMockServer
