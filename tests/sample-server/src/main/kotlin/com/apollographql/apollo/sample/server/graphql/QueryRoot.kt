package com.apollographql.apollo.sample.server.graphql

import com.apollographql.execution.annotation.GraphQLQuery


@GraphQLQuery
class QueryRoot {
  fun random(): Int = 42
  fun zero(): Int = 0
  fun valueSharedWithSubscriptions(): Int = 0
  fun secondsSinceEpoch(): Double = System.currentTimeMillis().div(1000).toDouble()
}


