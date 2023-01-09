package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo.cache.normalized.lru.EvictionPolicy
import com.apollographql.apollo.cache.normalized.lru.LruNormalizedCacheFactory
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import java.util.concurrent.TimeUnit

suspend fun main() {
  class MyData : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller = TODO()
  }

  class MyVariables : Operation.Variables()

  val cacheFactory1 = LruNormalizedCacheFactory(
    evictionPolicy = EvictionPolicy.builder().maxSizeBytes(10 * 1024 * 1024).build()
  )
  val cacheFactory2 = LruNormalizedCacheFactory(
    EvictionPolicy.builder().maxSizeBytes(10 * 1024 * 1024).build()
  )
  val cacheFactory3 = LruNormalizedCacheFactory(
    EvictionPolicy.builder()
      .maxSizeBytes(10 * 1024 * 1024)
      .expireAfterWrite(10, TimeUnit.MILLISECONDS)
      .build()
  )
  val cacheFactory4 = LruNormalizedCacheFactory(
    EvictionPolicy.builder()
      .maxSizeBytes(10 * 1024 * 1024)
      .expireAfterWrite(10, TimeUnit.MILLISECONDS)
      .expireAfterAccess(10, TimeUnit.MILLISECONDS)
      .maxEntries(42)
      .build()
  )
  val cacheFactory5 = LruNormalizedCacheFactory(
    EvictionPolicy.builder()
      .expireAfterWrite(10, TimeUnit.HOURS)
      .build()
  )

  val apolloClient = ApolloClient.builder()
    .normalizedCache(cacheFactory1)
    .build()

  val myQuery: Query<MyData, Any, MyVariables>? = null

  apolloClient!!
    .query(myQuery!!)
    .toBuilder()
    .responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
    .build()

  val cachedData = apolloClient
    .apolloStore
    .read(myQuery)
    .execute()

  val data: MyData? = null
  apolloClient
    .apolloStore
    .writeAndPublish(myQuery, data!!)
    .execute()

  apolloClient.clearNormalizedCache()
}
