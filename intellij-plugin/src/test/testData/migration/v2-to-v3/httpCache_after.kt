package com.example

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.http.HttpFetchPolicy
import com.apollographql.apollo3.cache.http.ApolloHttpCache
import java.io.File
import com.apollographql.apollo3.cache.http.httpFetchPolicy
import com.apollographql.apollo3.cache.http.httpCache

suspend fun main() {
  val maxSize = 10000L

  val apolloHttpCache3: ApolloHttpCache? = null

  val apolloClient = ApolloClient.Builder()
    .httpCache(File("cacheA"), maxSize)
    .httpCache(File("cacheB"), maxSize)
    .httpCache(File("cache1"), maxSize)
    .httpCache(File("cache2"), maxSize)
    .httpCache(/* TODO: This could not be migrated automatically. Please check the migration guide at https://www.apollographql.com/docs/kotlin/migration/3.0/ */)
    .httpCache(File("cache4"), maxSize)
    .httpCache(File("cache5"), maxSize)
    .build()

  val myQuery: Query<*, *, *>? = null
  apolloClient!!
    .query(myQuery!!)
    .httpFetchPolicy(HttpFetchPolicy.NetworkOnly)

  apolloClient.httpCache.clearAll()
}
