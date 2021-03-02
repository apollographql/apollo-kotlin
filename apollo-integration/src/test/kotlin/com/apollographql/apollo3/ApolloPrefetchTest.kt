package com.apollographql.apollo3

import com.apollographql.apollo3.Utils.assertResponse
import com.apollographql.apollo3.Utils.enqueueAndAssertResponse
import com.apollographql.apollo3.Utils.immediateExecutor
import com.apollographql.apollo3.Utils.immediateExecutorService
import com.apollographql.apollo3.Utils.mockResponse
import com.apollographql.apollo3.Utils.readFileToString
import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.cache.http.ApolloHttpCache
import com.apollographql.apollo3.cache.http.DiskLruHttpCacheStore
import com.apollographql.apollo3.cache.http.internal.FileSystem
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.rx2.Rx2Apollo
import com.google.common.truth.Truth
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApolloPrefetchTest {
  private lateinit var apolloClient: ApolloClient
  private lateinit var cacheStore: MockHttpCacheStore
  private lateinit var okHttpClient: OkHttpClient

  val server = MockWebServer()

  val inMemoryFileSystem = InMemoryFileSystem()

  var  lastHttRequest: Request? = null
  var lastHttResponse: okhttp3.Response? = null

  @Before
  fun setup() {
    cacheStore = MockHttpCacheStore()
    cacheStore.delegate = DiskLruHttpCacheStore(inMemoryFileSystem, File("/cache/"), Int.MAX_VALUE.toLong())
    okHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
          lastHttRequest = chain.request()
          lastHttResponse = chain.proceed(lastHttRequest)
          lastHttResponse
        }
        .dispatcher(Dispatcher(immediateExecutorService()))
        .build()
    apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .dispatcher(immediateExecutor())
        .httpCache(ApolloHttpCache(cacheStore, null))
        .build()
  }

  @After
  fun tearDown() {
    apolloClient.clearHttpCache()
  }

  @Test
  @Throws(IOException::class, ApolloException::class)
  fun prefetchDefault() {
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"))
    prefetch(apolloClient.prefetch(AllPlanetsQuery()))
    checkCachedResponse("HttpCacheTestAllPlanets.json")
    assertResponse(
        apolloClient
            .query(AllPlanetsQuery())
            .httpCachePolicy(HttpCachePolicy.CACHE_ONLY.expireAfter(2, TimeUnit.SECONDS))
    ) { dataResponse -> !dataResponse.hasErrors() }
  }

  @Test
  @Throws(Exception::class)
  fun prefetchNoCacheStore() {
    val apolloClient = ApolloClient.builder()
        .serverUrl(server.url("/"))
        .okHttpClient(okHttpClient)
        .dispatcher(immediateExecutor())
        .build()
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"))
    prefetch(apolloClient.prefetch(AllPlanetsQuery()))
    enqueueAndAssertResponse(
        server,
        "HttpCacheTestAllPlanets.json",
        apolloClient.query(AllPlanetsQuery())
    ) { response -> !response.hasErrors() }
  }

  @Test
  @Throws(IOException::class)
  fun prefetchFileSystemWriteFailure() {
    val faultyCacheStore = FaultyHttpCacheStore(FileSystem.SYSTEM)
    cacheStore.delegate = faultyCacheStore
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_HEADER_WRITE)
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"))
    Rx2Apollo.from(apolloClient.prefetch(AllPlanetsQuery()))
        .test()
        .assertError(Exception::class.java)
    checkNoCachedResponse()
    server.enqueue(mockResponse("HttpCacheTestAllPlanets.json"))
    faultyCacheStore.failStrategy(FaultyHttpCacheStore.FailStrategy.FAIL_BODY_WRITE)
    Rx2Apollo.from(apolloClient.prefetch(AllPlanetsQuery()))
        .test()
        .assertError(Exception::class.java)
    checkNoCachedResponse()
  }

  @Throws(IOException::class)
  private fun checkCachedResponse(fileName: String) {
    val cacheKey = lastHttRequest!!.headers(HttpCache.CACHE_KEY_HEADER)[0]
    val response = apolloClient.cachedHttpResponse(cacheKey)
    Truth.assertThat(response).isNotNull()
    Truth.assertThat(response!!.body()!!.source().readUtf8()).isEqualTo(readFileToString(javaClass, "/$fileName"))
    response.body()!!.source().close()
  }

  @Throws(IOException::class)
  private fun checkNoCachedResponse() {
    val cacheKey = lastHttRequest!!.header(HttpCache.CACHE_KEY_HEADER)
    val cachedResponse = apolloClient.cachedHttpResponse(cacheKey)
    Truth.assertThat(cachedResponse).isNull()
  }

  companion object {
    private fun prefetch(prefetch: ApolloPrefetch) {
      Rx2Apollo.from(prefetch).test()
    }
  }
}
