package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.coroutines.toFlow

suspend fun main() {
  val apolloClient: ApolloClient? = null
  val myQuery: Query<*, *, *>? = null
  val flow = apolloClient!!.query(myQuery!!)
    .watcher()
    .toFlow()
}
