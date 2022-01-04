package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.mockserver.MockDispatcher
import com.apollographql.apollo3.mockserver.MockRecordedRequest
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer

interface ApolloMockDispatcher {
  val customScalarAdapters: CustomScalarAdapters
  fun dispatch(request: MockRecordedRequest): ApolloResponse<out Operation.Data>
}

@ApolloExperimental
fun MockServer(apolloMockDispatcher: ApolloMockDispatcher) = MockServer(ApolloMockDispatcherBridge(apolloMockDispatcher))

class ApolloMockDispatcherBridge(val wrapped: ApolloMockDispatcher) : MockDispatcher {
  override fun dispatch(request: MockRecordedRequest): MockResponse {
    val apolloResponse = wrapped.dispatch(request)

    @Suppress("UNCHECKED_CAST")
    val responseBody = buildJsonString { apolloResponse.composeJsonResponse(this, wrapped.customScalarAdapters) }
    return MockResponse(responseBody)
  }

  override fun copy(): ApolloMockDispatcherBridge {
    return ApolloMockDispatcherBridge(wrapped)
  }
}
