package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.coroutines.await
import com.apollographql.apollo.coroutines.toFlow

suspend fun main() {
  val apolloClient = ApolloClient.builder()
    .serverUrl("http://example.com")
    .build()

  val myMutation: Mutation<*, *, *>? = null
  apolloClient.mutate(myMutation!!).await()

  val mySubscription: Subscription<*, *, *>? = null
  apolloClient.subscribe(mySubscription!!).toFlow()
}
