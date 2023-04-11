package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.AnyApolloAdapter
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ScalarAdapters
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.api.toJson
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue

fun <D : Operation.Data> MockServer.enqueue(
    operation: Operation<D>,
    data: D,
    scalarAdapters: ScalarAdapters = ScalarAdapters.Empty,
    delayMs: Long = 0
) {
  val json = buildJsonString {
    operation.composeJsonResponse(jsonWriter = this, data = data, scalarAdapters = scalarAdapters)
  }
  enqueue(json, delayMs)
}

fun MockServer.enqueueData(
    data: Map<String, Any?>,
    scalarAdapters: ScalarAdapters = ScalarAdapters.Empty,
    delayMs: Long = 0,
    statusCode: Int = 200
) {

  val response = buildJsonString {
    AnyApolloAdapter.toJson(this, scalarAdapters, mapOf("data" to data))
  }

  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(response)
      .delayMillis(delayMs)
      .build())
}


fun MockServer.enqueueData(
    data: Operation.Data,
    scalarAdapters: ScalarAdapters = ScalarAdapters.Empty,
    delayMs: Long = 0,
    statusCode: Int = 200
) {
  val response = buildJsonString {
    beginObject()
    name("data")
    data.toJson(this, scalarAdapters)
    endObject()
  }
  enqueue(MockResponse.Builder()
      .statusCode(statusCode)
      .body(response)
      .delayMillis(delayMs)
      .build())
}
