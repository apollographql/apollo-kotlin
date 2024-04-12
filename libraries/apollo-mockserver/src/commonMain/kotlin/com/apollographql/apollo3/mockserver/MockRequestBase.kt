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

  /**
   * The request headers
   *
   * Names are copied as-is from the wire. Since headers are case-insensitive, use [headerValueOf] to retrieve values.
   * Values are trimmed.
   */
  val headers: Map<String, String>
}

/**
 * Retrieves the value of the given key, using a case-insensitive matching
 */
fun Map<String, String>.headerValueOf(name: String): String? {
  return entries.firstOrNull { it.key.compareTo(name, ignoreCase = true) == 0 }?.value
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

  suspend fun awaitMessageOrClose(timeout: Duration = 1.seconds): Result<WebSocketMessage> {
    return withTimeout(timeout) {
      messages.receive()
    }
  }

  suspend fun awaitMessage(timeout: Duration = 1.seconds): WebSocketMessage {
    val result = awaitMessageOrClose(timeout)
    check(result.isSuccess) {
      "A message was expected but the socket was closed instead"
    }
    return result.getOrThrow()
  }

  suspend fun awaitClose(timeout: Duration = 1.seconds) {
    val result = awaitMessageOrClose(timeout)
    check(result.isFailure) {
      "Socket closed was expected but got a message instead"
    }
  }

  internal val messages = Channel<Result<WebSocketMessage>>(Channel.UNLIMITED)
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

