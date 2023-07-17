package com.example.myapplication

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloCompositeException

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

  val compositeException: ApolloCompositeException? = null
  println(compositeException!!.suppressedExceptions.first())
  println(compositeException!!.suppressedExceptions.getOrNull(1))
}
