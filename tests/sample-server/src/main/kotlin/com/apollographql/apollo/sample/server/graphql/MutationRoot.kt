package com.apollographql.apollo.sample.server.graphql

import com.apollographql.apollo.sample.server.WebSocketRegistry
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.execution.annotation.GraphQLMutation

@GraphQLMutation
class MutationRoot {
  fun closeAllWebSockets(executionContext: ExecutionContext): String {
    val registry = executionContext[WebSocketRegistry]

    val closed = registry!!.closeAllWebSockets()
    return "Closed $closed session(s)"
  }
}


