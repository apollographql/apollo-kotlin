package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Subscription

suspend fun main() {
  val apolloClient = ApolloClient.Builder()
    .serverUrl("http://example.com")
    .build()

  val myMutation: Mutation<*, *, *>? = null
  apolloClient.mutation(myMutation!!).execute()

  val mySubscription: Subscription<*, *, *>? = null
  apolloClient.subscription(mySubscription!!).toFlow()
}
