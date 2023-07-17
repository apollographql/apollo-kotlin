package com.example.myapplication

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.watch

suspend fun test() {
  val response: ApolloResponse<*>? = null
  println(response?.dataOrThrow())

  val apolloClient: ApolloClient? = null
  val query: Query<*>? = null

  apolloClient!!.query(query!!).toFlow()

  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).watch()
}
