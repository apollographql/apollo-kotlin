package com.apollographql.apollo.api.http

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation

/**
 * [WebSocketPayloadComposer] transforms a GraphQL request in a payload to be sent in a WebSocket message
 *
 * See [HttpRequestComposer]
 */
@ApolloExperimental
interface WebSocketPayloadComposer {
  fun <D : Operation.Data> compose(apolloRequest: ApolloRequest<D>): Map<String, Any?>
}

