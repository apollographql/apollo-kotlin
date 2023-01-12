package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory

suspend fun main() {
  val dbName = "apollo.db"
  val cacheFactory1 = SqlNormalizedCacheFactory(context, "apollo.db")
  val cacheFactory2 = SqlNormalizedCacheFactory(context, dbName)
  val cacheFactory3 = SqlNormalizedCacheFactory(dbName())
  val cacheFactory4 = SqlNormalizedCacheFactory(context, someInt())
  val cacheFactory5 = SqlNormalizedCacheFactory(a, b, c, d)

  val apolloClient = ApolloClient.builder()
    .normalizedCache(cacheFactory1)
    .build()
}

fun dbName(): String {
  return "apollo.db"
}

fun someInt(): Int {
  return 1
}
