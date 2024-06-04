package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.AnyAdapter
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.json.jsonReader
import com.apollographql.apollo3.api.json.readAny
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.TextMessage
import com.apollographql.apollo3.mockserver.WebsocketMockRequest
import com.apollographql.apollo3.mockserver.enqueueString
import okio.Buffer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated(
    "This is only used for internal Apollo tests and will be removed in a future version.",
    ReplaceWith(
        "enqueueString(operation.composeJsonResponse(data, customScalarAdapters), delayMs)",
        "com.apollographql.apollo3.mockserver.enqueueString",
        "com.apollographql.apollo3.api.composeJsonResponse",
    )
)
fun <D : Operation.Data> MockServer.enqueue(
    operation: Operation<D>,
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    delayMs: Long = 0,
) {
  val json = buildJsonString {
    operation.composeJsonResponse(jsonWriter = this, data = data, customScalarAdapters = customScalarAdapters)
  }
  enqueueString(json, delayMs)
}

@Deprecated(
    "This is only used for internal Apollo tests and will be removed in a future version.",
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun MockServer.enqueueData(
    data: Map<String, Any?>,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    delayMillis: Long = 0,
    statusCode: Int = 200,
) {

  val response = buildJsonString {
    AnyAdapter.toJson(this, customScalarAdapters, mapOf("data" to data))
  }

  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(response)
      .delayMillis(delayMillis)
      .build()
  )
}

@Deprecated(
    "This is only used for internal Apollo tests and will be removed in a future version.",
    ReplaceWith(
        "enqueueString(data.toResponseJson(customScalarAdapters), delayMillis, statusCode)",
        "com.apollographql.apollo3.mockserver.enqueueString",
        "com.apollographql.apollo3.api.toResponseJson",
    )
)
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
fun MockServer.enqueueData(
    data: Operation.Data,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    delayMillis: Long = 0,
    statusCode: Int = 200,
) {
  val response = buildJsonString {
    beginObject()
    name("data")
    data.toJson(this, customScalarAdapters)
    endObject()
  }
  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(response)
      .delayMillis(delayMillis)
      .build()
  )
}

/**
 * Extracts the operationId from a graphql-ws message
 */
@ApolloExperimental
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

@ApolloExperimental
fun connectionAckMessage(): String = "{\"type\": \"connection_ack\"}"

