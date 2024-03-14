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
  val call = apolloClient!!.mutate(myMutation!!)
  // await() -> execute() doesn't work due to https://youtrack.jetbrains.com/issue/KTIJ-29093/ but actually works in AS
  // call.await()

  val mySubscription: Subscription<*, *, *>? = null
  apolloClient.subscribe(mySubscription!!).toFlow()
}
