package com.apollographql.apollo.sample.server.graphql

import com.apollographql.apollo.sample.server.WebsocketRegistry
import com.apollographql.apollo3.annotations.ApolloObject
import com.apollographql.apollo3.api.ExecutionContext


@ApolloObject
class Query {
  fun random(): Int = 42
  fun time(): Int = 0

  fun closeWebSocket(executionContext: ExecutionContext): String {
    val registry = executionContext[WebsocketRegistry]

    val closed = registry!!.closeAllSessions()
    return "Closed ${closed} session(s)"
  }
}


