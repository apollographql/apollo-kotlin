package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.normalizedCache

suspend fun main() {
  val dbName = "apollo.db"
  val cacheFactory1 = SqlNormalizedCacheFactory("apollo.db")
  val cacheFactory2 = SqlNormalizedCacheFactory(dbName)
  val cacheFactory3 = SqlNormalizedCacheFactory(dbName())
  val cacheFactory4 = SqlNormalizedCacheFactory(context, someInt())
  val cacheFactory5 = SqlNormalizedCacheFactory(a, b, c, d)

  val apolloClient = ApolloClient.Builder()
    .normalizedCache(cacheFactory1)
    .build()
}

fun dbName(): String {
  return "apollo.db"
}

fun someInt(): Int {
  return 1
}
