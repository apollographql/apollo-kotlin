package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

@ApolloExperimental
class DefaultWebSocketPayloadComposer: WebSocketPayloadComposer {
  override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): Map<String, Any?> {
    return DefaultHttpRequestComposer.composePayload(apolloRequest)
  }
}