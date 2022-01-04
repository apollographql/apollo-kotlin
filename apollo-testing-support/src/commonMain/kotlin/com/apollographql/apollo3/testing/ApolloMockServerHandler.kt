package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.mockserver.MockRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.MockServerHandler

interface ApolloMockServerHandler {
  val customScalarAdapters: CustomScalarAdapters
  fun handle(request: MockRequest): ApolloResponse<out Operation.Data>
}

@ApolloExperimental
fun MockServer(apolloMockServerHandler: ApolloMockServerHandler) = MockServer(ApolloMockServerHandlerBridge(apolloMockServerHandler))

class ApolloMockServerHandlerBridge(val wrapped: ApolloMockServerHandler) : MockServerHandler {
  override fun handle(request: MockRequest): MockResponse {
    val apolloResponse = wrapped.handle(request)

    @Suppress("UNCHECKED_CAST")
    val responseBody = buildJsonString { apolloResponse.composeJsonResponse(this, wrapped.customScalarAdapters) }
    return MockResponse(responseBody)
  }

  override fun copy(): ApolloMockServerHandlerBridge {
    return ApolloMockServerHandlerBridge(wrapped)
  }
}
