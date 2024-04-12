package com.apollographql.apollo3.testing

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
import com.apollographql.apollo3.mockserver.WebSocketMessage
import com.apollographql.apollo3.mockserver.enqueueString
import okio.Buffer

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
      .build())
}


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
      .build())
}

/**
 * Extracts the operationId from a graphql-ws message
 */
@ApolloExperimental
fun WebSocketMessage.operationId(): String {
  if (this !is TextMessage) {
    TODO()
  }
  return (Buffer().writeUtf8(text).jsonReader().readAny() as Map<*, *>).get("id") as String
}


@ApolloExperimental
fun connectionAckMessage(): WebSocketMessage = TextMessage("{\"type\": \"connection_ack\"}")

