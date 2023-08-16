package com.apollographql.apollo3.mockserver

import kotlinx.coroutines.flow.Flow
import kotlin.random.Random

interface WebSocketMockServer {
  sealed class WebSocketEvent {
    class Connect(val sessionId: String, val headers: Map<String, String>) : WebSocketEvent()
    class TextFrame(val sessionId: String, val text: String) : WebSocketEvent()
    class BinaryFrame(val sessionId: String, val bytes: ByteArray) : WebSocketEvent()
    class Close(val sessionId: String, val reasonCode: Short?, val reasonMessage: String?) : WebSocketEvent()
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

expect fun WebSocketMockServer(port: Int = Random.nextInt(10000, 20000)): WebSocketMockServer
