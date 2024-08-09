package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.internal.ResponseFieldMarshaller
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import java.util.concurrent.TimeUnit
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.apolloStore
import com.apollographql.apollo3.cache.normalized.normalizedCache

suspend fun main() {
  class MyData : Operation.Data {
    override fun marshaller(): ResponseFieldMarshaller = TODO()
  }

  class MyVariables : Operation.Variables()

  val cacheFactory1 = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)
  val cacheFactory2 = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024)
  val cacheFactory3 =
    MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024, expireAfterMillis = TimeUnit.MILLISECONDS.toMillis(10))
  val cacheFactory4 =
    MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024, expireAfterMillis = TimeUnit.MILLISECONDS.toMillis(10))
  val cacheFactory5 = MemoryCacheFactory(expireAfterMillis = TimeUnit.HOURS.toMillis(10))

  val apolloClient = ApolloClient.Builder()
    .normalizedCache(cacheFactory1)
    .build()

  val myQuery: Query<MyData, Any, MyVariables>? = null

  apolloClient!!
    .query(myQuery!!)
    .fetchPolicy(FetchPolicy.NetworkOnly)

  val cachedData = apolloClient
    .apolloStore
    .readOperation(myQuery)

  val data: MyData? = null
  apolloClient
    .apolloStore
    .writeOperation(myQuery, data!!)

  apolloClient.apolloStore.clearAll()
}
