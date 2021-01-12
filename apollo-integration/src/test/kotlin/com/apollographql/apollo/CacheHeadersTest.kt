package com.apollographql.apollo

import com.apollographql.apollo.Utils.immediateExecutor
import com.apollographql.apollo.Utils.immediateExecutorService
import com.apollographql.apollo.Utils.readFileToString
import com.apollographql.apollo.api.Input.Companion.fromNullable
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.CacheHeaders.Companion.builder
import com.apollographql.apollo.cache.normalized.*
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo.integration.normalizer.type.Episode
import com.apollographql.apollo.rx2.Rx2Apollo
import com.google.common.truth.Truth
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

class CacheHeadersTest {
  val server = MockWebServer()
  @Test
  @Throws(ApolloException::class, IOException::class)
  fun testHeadersReceived() {
    val hasHeader = AtomicBoolean()
    val normalizedCache: NormalizedCache = object : NormalizedCache() {
      override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return null
      }

      override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
        return null
      }

      override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return emptySet<String>()
      }

      override fun clearAll() {}
      override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
        return false
      }

      override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
        return emptySet()
      }
    }
    val cacheFactory: NormalizedCacheFactory<NormalizedCache> = object : NormalizedCacheFactory<NormalizedCache>() {
      override fun create(recordFieldAdapter: RecordFieldJsonAdapter): NormalizedCache {
        return normalizedCache
      }
    }
    val apolloClient = ApolloClient.builder()
        .normalizedCache(cacheFactory, IdFieldCacheKeyResolver())
        .serverUrl(server.url("/"))
        .okHttpClient(OkHttpClient.Builder().dispatcher(Dispatcher(immediateExecutorService())).build())
        .dispatcher(immediateExecutor())
        .build()
    server.enqueue(mockResponse("HeroAndFriendsNameResponse.json"))
    val cacheHeaders = builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build()
    Rx2Apollo.from(apolloClient.query(HeroAndFriendsNamesQuery(fromNullable(Episode.NEWHOPE)))
        .cacheHeaders(cacheHeaders))
        .test()
    Truth.assertThat(hasHeader.get()).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun testDefaultHeadersReceived() {
    val hasHeader = AtomicBoolean()
    val normalizedCache: NormalizedCache = object : NormalizedCache() {
      override fun loadRecord(key: String, cacheHeaders: CacheHeaders): Record? {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return null
      }

      override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
        return null
      }

      override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return emptySet<String>()
      }

      override fun clearAll() {}
      override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
        return false
      }

      override fun performMerge(apolloRecord: Record, oldRecord: Record?, cacheHeaders: CacheHeaders): Set<String> {
        return emptySet()
      }
    }
    val cacheFactory: NormalizedCacheFactory<NormalizedCache> = object : NormalizedCacheFactory<NormalizedCache>() {
      override fun create(recordFieldAdapter: RecordFieldJsonAdapter): NormalizedCache {
        return normalizedCache
      }
    }
    val cacheHeaders = builder().addHeader(ApolloCacheHeaders.DO_NOT_STORE, "true").build()
    val apolloClient = ApolloClient.builder()
        .normalizedCache(cacheFactory, IdFieldCacheKeyResolver())
        .serverUrl(server.url("/"))
        .okHttpClient(OkHttpClient.Builder().dispatcher(Dispatcher(immediateExecutorService())).build())
        .dispatcher(immediateExecutor())
        .defaultCacheHeaders(cacheHeaders)
        .build()
    server.enqueue(mockResponse("HeroAndFriendsNameResponse.json"))
    Rx2Apollo.from(apolloClient.query(HeroAndFriendsNamesQuery(fromNullable(Episode.NEWHOPE)))
        .cacheHeaders(cacheHeaders))
        .test()
    Truth.assertThat(hasHeader.get()).isTrue()
  }

  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }
}