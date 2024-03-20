package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Subscription

suspend fun main() {
  val apolloClient = ApolloClient.Builder()
    .serverUrl("http://example.com")
    .build()

  val myMutation: Mutation<*, *, *>? = null
  val call = apolloClient!!.mutation(myMutation!!)
  // await() -> execute() doesn't work due to https://youtrack.jetbrains.com/issue/KTIJ-29093/ but actually works in AS
  // call.await()

  val mySubscription: Subscription<*, *, *>? = null
  apolloClient.subscription(mySubscription!!).toFlow()
}
