package com.example.myapplication

import com.apollographql.apollo3.ApolloClient

suspend fun test() {
  val apolloClient: ApolloClient? = ApolloClient.Builder().useV3ExceptionHandling(true)
      .serverUrl("http://localhost")
      .build()
}
