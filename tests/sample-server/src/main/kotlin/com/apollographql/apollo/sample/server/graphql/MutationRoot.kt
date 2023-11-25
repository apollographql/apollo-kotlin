package com.apollographql.apollo.sample.server.graphql

import com.apollographql.apollo.sample.server.WebSocketRegistry
import com.apollographql.apollo3.annotations.GraphQLObject
import com.apollographql.apollo3.api.ExecutionContext


@GraphQLObject(name = "Mutation")
class MutationRoot {
  fun closeAllWebSockets(executionContext: ExecutionContext): String {
    val registry = executionContext[WebSocketRegistry]

    val closed = registry!!.closeAllWebSockets()
    return "Closed $closed session(s)"
  }
}


