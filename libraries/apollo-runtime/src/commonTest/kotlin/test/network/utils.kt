package test.network

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.json.jsonReader
import com.apollographql.apollo.api.json.readAny
import com.apollographql.mockserver.TextMessage
import com.apollographql.mockserver.WebsocketMockRequest
import okio.Buffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Extracts the operationId from a graphql-ws message
 *
 * @param messagesToIgnore messages to ignore
 */
suspend fun WebsocketMockRequest.awaitSubscribe(timeout: Duration = 1.seconds, messagesToIgnore: Set<String> = emptySet()): String {
  while(true) {
    val message = awaitMessage(timeout)
    if (message !is TextMessage) {
      TODO()
    }
    val map = (Buffer().writeUtf8(message.text).jsonReader().readAny() as Map<*, *>)

    if (messagesToIgnore.contains(map["type"])) {
      continue
    }
    check(map["type"] == "subscribe") {
      "Expected subscribe, got '${map.get("type")}'"
    }
    return map.get("id") as String
  }
}


@ApolloExperimental
suspend fun WebsocketMockRequest.awaitComplete(timeout: Duration = 1.seconds) {
  val message = awaitMessage(timeout)
  if (message !is TextMessage) {
    TODO()
  }
  val map = (Buffer().writeUtf8(message.text).jsonReader().readAny() as Map<*, *>)

  check(map["type"] == "complete") {
    "Expected complete, got '${map.get("type")}"
  }
}

fun connectionAckMessage(): String = "{\"type\": \"connection_ack\"}"