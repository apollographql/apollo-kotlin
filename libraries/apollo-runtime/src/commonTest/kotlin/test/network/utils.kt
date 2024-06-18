package test.network

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.mockserver.TextMessage
import com.apollographql.mockserver.WebsocketMockRequest
import okio.Buffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

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

/**
 * Extracts the operationId from a graphql-ws message, ignores "complete messages"
 */
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



