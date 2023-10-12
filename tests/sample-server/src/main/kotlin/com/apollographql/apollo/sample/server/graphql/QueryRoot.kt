package com.apollographql.apollo.sample.server.graphql

import com.apollographql.apollo3.annotations.ApolloObject
import com.apollographql.apollo3.annotations.GraphQLName


@ApolloObject
@GraphQLName(name = "Query")
class QueryRoot {
  fun random(): Int = 42
  fun time(): Int = 0
}


