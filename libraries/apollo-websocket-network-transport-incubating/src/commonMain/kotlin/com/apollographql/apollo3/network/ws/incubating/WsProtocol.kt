package com.apollographql.apollo3.network.ws.incubating

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

interface WsProtocol {
  val name: String

  suspend fun connectionInit(): ClientMessage
  suspend fun <D : Operation.Data> operationStart(request: ApolloRequest<D>): ClientMessage
  fun <D : Operation.Data> operationStop(request: ApolloRequest<D>): ClientMessage
  fun ping(): ClientMessage?
  fun pong(): ClientMessage?

  fun parseServerMessage(text: String): ServerMessage
}

