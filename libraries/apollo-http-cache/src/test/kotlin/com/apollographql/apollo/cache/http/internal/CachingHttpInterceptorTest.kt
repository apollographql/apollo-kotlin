package com.apollographql.apollo.cache.http.internal

import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.http.valueOf
import com.apollographql.apollo.cache.http.CachingHttpInterceptor
import com.apollographql.apollo.exception.HttpCacheMissException
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpInterceptorChain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("BlockingMethodInNonBlockingContext")
class CachingHttpInterceptorTest {
  private lateinit var mockServer: MockServer
  private lateinit var interceptor: CachingHttpInterceptor
  private lateinit var chain: HttpInterceptorChain

  @Before
  fun before() {
    mockServer = MockServer()
    val dir = File("build/httpCache")
    dir.deleteRecursively()
    interceptor = CachingHttpInterceptor(dir, Long.MAX_VALUE)
    chain = TestHttpInterceptorChain()
  }

  @Test
  fun successResponsesAreCached() {
    val body = "success"
    mockServer.enqueueString(body)

    runBlocking {
      val request = HttpRequest.Builder(
          method = HttpMethod.Get,
          url = mockServer.url(),
      )
          .withCacheKey()
          .build()

      var response = interceptor.intercept(request, chain)
      assertEquals(body, response.body?.readUtf8())

      // Cache is committed when the body is closed
      response.body?.close()

      // 2nd request should hit the cache
      response = interceptor.intercept(
          request.newBuilder()
              .addHeader(CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, CachingHttpInterceptor.CACHE_ONLY)
              .build(),
          chain
      )
      assertEquals(body, response.body?.readUtf8())
      assertEquals("true", response.headers.valueOf(CachingHttpInterceptor.FROM_CACHE))
    }
  }

  @Test
  fun failureResponsesAreNotCached() {
    mockServer.enqueue(MockResponse.Builder().statusCode(500).body("error").build())

    runBlocking {
      val request = HttpRequest.Builder(
          method = HttpMethod.Get,
          url = mockServer.url(),
      )
          .withCacheKey()
          .build()

      // Warm the cache
      val response = interceptor.intercept(request, chain)
      assertEquals("error", response.body?.readUtf8())

      // 2nd request should trigger a cache miss
      assertFailsWith(HttpCacheMissException::class) {
        interceptor.intercept(
            request.newBuilder()
                .addHeader(CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, CachingHttpInterceptor.CACHE_ONLY)
                .build(),
            chain
        )
      }
    }
  }

  @Test
  fun timeoutWorks() {
    val body = "success"
    mockServer.enqueueString(body)
    runBlocking {
      val request = HttpRequest.Builder(
          method = HttpMethod.Get,
          url = mockServer.url(),
      )
          .withCacheKey()
          .build()

      // Warm the cache
      var response = interceptor.intercept(request, chain)
      assertEquals(body, response.body?.readUtf8())
      // Cache is committed when the body is closed
      response.body?.close()

      // 2nd request should hit the cache
      response = interceptor.intercept(
          request.newBuilder()
              .addHeader(CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, CachingHttpInterceptor.CACHE_ONLY)
              .build(),
          chain
      )
      assertEquals(body, response.body?.readUtf8())
      // Cache is committed when the body is closed
      response.body?.close()

      delay(1000)
      // 3rd request with a 500ms timeout should miss
      assertFailsWith(HttpCacheMissException::class) {
        interceptor.intercept(
            request.newBuilder()
                .addHeader(CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, CachingHttpInterceptor.CACHE_ONLY)
                .addHeader(CachingHttpInterceptor.CACHE_EXPIRE_TIMEOUT_HEADER, "500")
                .build(),
            chain
        )
      }
    }
  }

  @Test
  fun cacheInParallel() {
    val concurrency = 2
    repeat(concurrency) {
      mockServer.enqueueString("success")
    }
    val jobs = mutableListOf<Job>()
    runBlocking {
      repeat(concurrency) {
        jobs += launch(Dispatchers.IO) {
          val request = HttpRequest.Builder(
              method = HttpMethod.Get,
              url = mockServer.url(),
          )
              .withCacheKey()
              .build()

          val response = interceptor.intercept(request, chain)
          assertEquals("success", response.body?.readUtf8())
        }
      }
      jobs.forEach { it.join() }
    }
  }
}

private fun HttpRequest.Builder.withCacheKey(): HttpRequest.Builder {
  val cacheKey = CachingHttpInterceptor.cacheKey(build())
  return addHeader(CachingHttpInterceptor.CACHE_KEY_HEADER, cacheKey)
}

private class TestHttpInterceptorChain : HttpInterceptorChain {
  val engine = DefaultHttpEngine()

  override suspend fun proceed(request: HttpRequest): HttpResponse {
    return engine.execute(request)
  }
}
