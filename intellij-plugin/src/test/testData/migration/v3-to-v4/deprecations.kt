package com.example.myapplication

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.cache.http.DiskLruHttpCache
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.apolloStore
import com.apollographql.apollo.cache.normalized.emitCacheMisses
import com.apollographql.apollo.cache.normalized.executeCacheAndNetwork
import com.apollographql.apollo.cache.normalized.watch
import com.apollographql.apollo.exception.ApolloCanceledException
import com.apollographql.apollo.exception.ApolloCompositeException
import com.apollographql.apollo.exception.ApolloGenericException
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport

suspend fun test() {
  val response: ApolloResponse<*>? = null
  println(response?.dataAssertNoErrors)

  val apolloClient: ApolloClient? = ApolloClient.builder()
      .requestedDispatcher(null)
      .webSocketReconnectWhen { true }
      .webSocketReconnectWhen { a -> true }
      .build()
  val query: Query<*>? = null

  apolloClient!!.query(query!!).emitCacheMisses(true).toFlow()

  apolloClient!!.query(query!!).watch(true, false)
  apolloClient!!.query(query!!).watch(true)
  apolloClient!!.query(query!!).watch(fetchThrows = true, refetchThrows = false)
  apolloClient!!.query(query!!).watch(fetchThrows = true)
  apolloClient!!.query(query!!).watch()
  apolloClient!!.query(query!!).execute()
  apolloClient!!.query(query!!).toFlow()

  val compositeException: ApolloCompositeException? = null
  println(compositeException!!.first)
  println(compositeException!!.second)

  val first = Exception("first")
  throw ApolloCompositeException(first = first, Exception("second"))

  apolloClient!!.query(query!!).executeCacheAndNetwork()

  apolloClient!!.apolloStore.clearAll()

  // apolloStore() -> apolloStore doesn't work due to https://youtrack.jetbrains.com/issue/KTIJ-29078/
  // apolloClient!!.apolloStore().clearAll()

  val cacheHeaders: CacheHeaders = CacheHeaders.NONE.toBuilder().build()

  val cacheKey1 = CacheKey.from("typeName", listOf("a"))
  val cacheKey2 = CacheKey.from("typeName", "a", "b")

  apolloClient.dispose()

  val mutation: Mutation<*>? = null
  apolloClient.mutate(mutation!!)

  val subscription: Subscription<*>? = null
  apolloClient.subscribe(subscription!!)

  val webSocketNetworkTransport = WebSocketNetworkTransport.Builder()
      .reconnectWhen { true }
      .reconnectWhen { a -> true }
      .build()

  try {
  } catch (e1: ApolloGenericException) {
  } catch (e2: ApolloCanceledException) {
  }

  val diskLruHttpCache: DiskLruHttpCache? = null
  diskLruHttpCache!!.delete()
}
