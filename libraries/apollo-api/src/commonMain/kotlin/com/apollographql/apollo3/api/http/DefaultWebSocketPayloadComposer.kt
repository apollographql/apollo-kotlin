package com.apollographql.apollo.api.http

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation

@ApolloExperimental
class DefaultWebSocketPayloadComposer: WebSocketPayloadComposer {
  override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): Map<String, Any?> {
    return DefaultHttpRequestComposer.composePayload(apolloRequest)
  }
}