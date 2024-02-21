package com.apollographql.apollo3.network.websocket

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.Operation

interface WsProtocol {
  val name: String

  suspend fun connectionInit(): ClientMessage
  suspend fun <D : Operation.Data> operationStart(request: ApolloRequest<D>): ClientMessage
  suspend fun <D : Operation.Data> operationStop(request: ApolloRequest<D>): ClientMessage
  suspend fun ping(): ClientMessage?
  suspend fun pong(): ClientMessage?

  fun parseServerMessage(text: String): ServerMessage

  interface Factory {
    fun build(): WsProtocol
  }
}

