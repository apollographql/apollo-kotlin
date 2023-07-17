package com.example.myapplication

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.FetchPolicy

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

  val compositeException: ApolloException? = null
  println(compositeException!!.suppressedExceptions.first())
  println(compositeException!!.suppressedExceptions.getOrNull(1))

  apolloClient!!.query(query!!).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow()
}
