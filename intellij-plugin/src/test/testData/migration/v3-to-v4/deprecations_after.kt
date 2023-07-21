package com.example.myapplication

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.cache.http.DiskLruHttpCache
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.FetchPolicy

suspend fun test() {
  val response: ApolloResponse<*>? = null
  println(response?.dataOrThrow())

  val apolloClient: ApolloClient? = ApolloClient.Builder()
      .dispatcher(null)
      .webSocketReopenWhen { it, _ -> true }
      .webSocketReopenWhen { a, _ -> true }
      .build()
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

  apolloClient!!.apolloStore.clearAll()

  apolloClient!!.apolloStore.clearAll()

  val cacheHeaders: CacheHeaders = CacheHeaders.NONE.newBuilder().build()

  val cacheKey1 = CacheKey("typeName", listOf("a"))
  val cacheKey2 = CacheKey("typeName", "a", "b")

  apolloClient.close()

  val mutation: Mutation<*>? = null
  apolloClient.mutation(mutation!!)

  val subscription: Subscription<*>? = null
  apolloClient.subscription(subscription!!)

  val webSocketNetworkTransport = WebSocketNetworkTransport.Builder()
      .reopenWhen { it, _ -> true }
      .reopenWhen { a, _ -> true }
      .build()

  try {
  } catch (e1: ApolloException) {
  } catch (e2: ApolloException) {
  }

  val diskLruHttpCache: DiskLruHttpCache? = null
  diskLruHttpCache!!.clearAll()
}
