package com.example.myapplication

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.emitCacheMisses
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloCompositeException

suspend fun test() {
  val response: ApolloResponse<*>? = null
  println(response?.dataAssertNoErrors)

  val apolloClient: ApolloClient? = null
  val query: Query<*>? = null

  apolloClient!!.query(query!!).emitCacheMisses(true).toFlow()

  apolloClient!!.query(query!!).watch(true, false)
  apolloClient!!.query(query!!).watch(true)
  apolloClient!!.query(query!!).watch(fetchThrows = true, refetchThrows = false)
  apolloClient!!.query(query!!).watch(fetchThrows = true)
  apolloClient!!.query(query!!).watch()

  val compositeException: ApolloCompositeException? = null
  println(compositeException!!.first)
  println(compositeException!!.second)
}
