package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

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
  fun url(): String

  val events: Flow<WebSocketEvent>

  suspend fun sendText(sessionId: String, text: String)
  suspend fun sendBinary(sessionId: String, binary: ByteArray)
  suspend fun sendClose(sessionId: String, reasonCode: Short? = null, reasonMessage: String? = null)
  fun stop()
}

@ApolloExperimental
expect fun WebSocketMockServer(port: Int = Random.nextInt(10000, 20000)): WebSocketMockServer
