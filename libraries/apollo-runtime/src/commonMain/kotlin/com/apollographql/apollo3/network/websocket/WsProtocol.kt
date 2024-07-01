package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation

/**
 * A [WsProtocol] manages different flavours of WebSocket protocols.
 *
 * See [GraphQLWsProtocol], [AppSyncWsProtocol] and [SubscriptionWsProtocol]
 */
@ApolloExperimental
interface WsProtocol {
  val name: String

  suspend fun connectionInit(): ClientMessage
  suspend fun <D : Operation.Data> operationStart(request: ApolloRequest<D>): ClientMessage
  fun <D : Operation.Data> operationStop(request: ApolloRequest<D>): ClientMessage
  fun ping(): ClientMessage?
  fun pong(): ClientMessage?

  fun parseServerMessage(text: String): ServerMessage
}

