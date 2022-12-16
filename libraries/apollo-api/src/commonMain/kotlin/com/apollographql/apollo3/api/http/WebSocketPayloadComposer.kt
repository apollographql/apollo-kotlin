package com.apollographql.apollo3.api.http

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

/**
 * [WebSocketPayloadComposer] transforms a GraphQL request in a payload to be sent in a WebSocket message
 *
 * See [HttpRequestComposer]
 */
@ApolloExperimental
interface WebSocketPayloadComposer {
  fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): Map<String, Any?>
}

