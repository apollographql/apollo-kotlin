package com.apollographql.apollo3.mockserver

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout
import okio.ByteString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

interface MockRequestBase {
  val method: String
  val path: String
  val version: String
  val headers: Map<String, String>
}

class MockRequest(
    override val method: String,
    override val path: String,
    override val version: String,
    override val headers: Map<String, String> = emptyMap(),
    val body: ByteString = ByteString.EMPTY,
) : MockRequestBase

@ApolloExperimental
class WebsocketMockRequest(
    override val method: String,
    override val path: String,
    override val version: String,
    override val headers: Map<String, String> = emptyMap(),
) : MockRequestBase {

  suspend fun awaitMessage(timeout: Duration = 1.seconds): WebSocketMessage {
    return withTimeout(timeout) {
      messages.receive()
    }
  }

  internal val messages = Channel<WebSocketMessage>(Channel.UNLIMITED)
}

@ApolloExperimental
sealed interface WebSocketMessage

@ApolloExperimental
class TextMessage(val text: String) : WebSocketMessage
@ApolloExperimental
class DataMessage(val data: ByteArray) : WebSocketMessage
@ApolloExperimental
class CloseFrame(val code: Int?, val reason: String?) : WebSocketMessage
@ApolloExperimental
data object PingFrame : WebSocketMessage
@ApolloExperimental
data object PongFrame : WebSocketMessage
