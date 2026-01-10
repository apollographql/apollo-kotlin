package com.apollographql.apollo.api.http

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.toMap
import com.apollographql.apollo.api.toRequestParameters

@ApolloExperimental
class DefaultWebSocketPayloadComposer: WebSocketPayloadComposer {
  override fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): Map<String, Any?> {
    return apolloRequest.toRequestParameters().toMap()
  }
}