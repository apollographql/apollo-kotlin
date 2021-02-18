package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.readFileToString
import com.apollographql.apollo3.api.Input
import com.apollographql.apollo3.api.internal.json.JsonReader
import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.CacheHeaders.Companion.builder
import com.apollographql.apollo3.cache.normalized.*
import com.apollographql.apollo3.coroutines.await
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.integration.normalizer.HeroAndFriendsNamesQuery
import com.apollographql.apollo3.integration.normalizer.type.Episode
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth
import kotlinx.coroutines.runBlocking
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.reflect.KClass

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

      override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return emptySet<String>()
      }

      override fun clearAll() {}

      override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
        return false
      }

      override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return emptyList()
      }

      override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return emptySet()
      }

      override fun dump(): Map<@JvmSuppressWildcards KClass<*>, Map<String, Record>> {
        return emptyMap()
      }
    }
    val cacheFactory: NormalizedCacheFactory<NormalizedCache> = object : NormalizedCacheFactory<NormalizedCache>() {
      override fun create(): NormalizedCache {
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
    Rx2Apollo.from(apolloClient.query(HeroAndFriendsNamesQuery(Input.present(Episode.NEWHOPE)))
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

      override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return emptySet<String>()
      }

      override fun clearAll() {}

      override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
        return false
      }

      override fun loadRecords(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return emptyList()
      }

      override fun merge(records: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
        hasHeader.set(cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE))
        return emptySet()
      }

      override fun dump(): Map<@JvmSuppressWildcards KClass<*>, Map<String, Record>> {
        return emptyMap()
      }
    }
    val cacheFactory: NormalizedCacheFactory<NormalizedCache> = object : NormalizedCacheFactory<NormalizedCache>() {
      override fun create(): NormalizedCache {
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

    runBlocking {
      apolloClient.query(HeroAndFriendsNamesQuery(Input.present(Episode.NEWHOPE)))
          .cacheHeaders(cacheHeaders)
          .await()
    }
    Truth.assertThat(hasHeader.get()).isTrue()
  }

  @Throws(IOException::class)
  private fun mockResponse(fileName: String): MockResponse {
    return MockResponse().setChunkedBody(readFileToString(javaClass, "/$fileName"), 32)
  }
}
