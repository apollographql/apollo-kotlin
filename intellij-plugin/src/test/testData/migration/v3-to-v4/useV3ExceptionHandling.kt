package com.example.myapplication

import com.apollographql.apollo3.ApolloClient

suspend fun test() {
  val apolloClient: ApolloClient? = ApolloClient.Builder()
      .serverUrl("http://localhost")
      .build()
}
