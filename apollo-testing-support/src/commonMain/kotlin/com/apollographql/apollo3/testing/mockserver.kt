package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue

@OptIn(ApolloInternal::class)
fun <D : Operation.Data> MockServer.enqueue(
    operation: Operation<D>,
    data: D,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    delayMs: Long = 0
) {
  val json = buildJsonString {
    operation.composeJsonResponse(jsonWriter = this, data = data, customScalarAdapters = customScalarAdapters)
  }
  enqueue(json, delayMs)
}

