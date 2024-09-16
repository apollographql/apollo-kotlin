package com.example.myapplication

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.cache.http.DiskLruHttpCache
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.watch
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.cache.normalized.fetchPolicy
import com.apollographql.apollo.cache.normalized.FetchPolicy
import com.apollographql.apollo.cache.normalized.apolloStore

suspend fun test() {
  val response: ApolloResponse<*>? = null
  println(response?.dataAssertNoErrors)

  val apolloClient: ApolloClient? = ApolloClient.Builder()
      .dispatcher(null)
      .webSocketReopenWhen { it, _ -> true }
      .webSocketReopenWhen { a, _ -> true }
      .build()
  val query: Query<*>? = null

  apolloClient!!.query(query!!).toFlowV3()

  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).executeV3()
  apolloClient!!.query(query!!).toFlowV3()

  val compositeException: ApolloException? = null
  println(compositeException!!.suppressedExceptions.first())
  println(compositeException!!.suppressedExceptions.getOrNull(1))

  val first = Exception("first")
  throw DefaultApolloException(cause = first).apply { addSuppressed(Exception("second")) }

  apolloClient!!.query(query!!).fetchPolicy(FetchPolicy.CacheAndNetwork).toFlow()

  apolloClient!!.apolloStore.clearAll()

  // apolloStore() -> apolloStore doesn't work due to https://youtrack.jetbrains.com/issue/KTIJ-29078/
  // apolloClient!!.apolloStore().clearAll()

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
