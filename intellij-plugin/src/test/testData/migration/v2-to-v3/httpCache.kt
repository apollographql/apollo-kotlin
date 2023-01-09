package com.example

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.cache.http.ApolloHttpCache
import com.apollographql.apollo.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo.cache.http.internal.FileSystem
import java.io.File

suspend fun main() {
  val maxSize = 10000L
  val apolloHttpCache1 = ApolloHttpCache(DiskLruHttpCacheStore(File("cache1"), maxSize))

  val apolloHttpCache2 = ApolloHttpCache(DiskLruHttpCacheStore(FileSystem.SYSTEM, File("cache2"), maxSize))

  val apolloHttpCache3: ApolloHttpCache? = null

  val diskLruHttpCacheStore1 = DiskLruHttpCacheStore(File("cache4"), maxSize)
  val apolloHttpCache4 = ApolloHttpCache(diskLruHttpCacheStore1)

  val diskLruHttpCacheStore2 = DiskLruHttpCacheStore(FileSystem.SYSTEM, File("cache5"), maxSize)
  val apolloHttpCache5 = ApolloHttpCache(diskLruHttpCacheStore2)

  val apolloClient = ApolloClient.builder()
    .httpCache(ApolloHttpCache(DiskLruHttpCacheStore(File("cacheA"), maxSize)))
    .httpCache(ApolloHttpCache(DiskLruHttpCacheStore(FileSystem.SYSTEM, File("cacheB"), maxSize)))
    .httpCache(apolloHttpCache1)
    .httpCache(apolloHttpCache2)
    .httpCache(apolloHttpCache3!!)
    .httpCache(apolloHttpCache4)
    .httpCache(apolloHttpCache5)
    .build()

  val myQuery: Query<*, *, *>? = null
  apolloClient!!
    .query(myQuery!!)
    .toBuilder()
    .httpCachePolicy(HttpCachePolicy.NETWORK_ONLY)
    .build()

  apolloClient.clearHttpCache()
}
