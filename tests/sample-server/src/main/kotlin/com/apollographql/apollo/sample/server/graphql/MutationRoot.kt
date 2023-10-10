package com.apollographql.apollo.sample.server.graphql

import com.apollographql.apollo.sample.server.WebSocketRegistry
import com.apollographql.apollo3.annotations.ApolloObject
import com.apollographql.apollo3.annotations.GraphQLName
import com.apollographql.apollo3.api.ExecutionContext


@ApolloObject
@GraphQLName(name = "Mutation")
class MutationRoot {
  fun closeAllWebSockets(executionContext: ExecutionContext): String {
    val registry = executionContext[WebSocketRegistry]

    val closed = registry!!.closeAllWebSockets()
    return "Closed $closed session(s)"
  }
}


