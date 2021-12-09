package com.apollographql.apollo3.cache.http.internal

import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.http.valueOf
import com.apollographql.apollo3.cache.http.CachingHttpInterceptor
import com.apollographql.apollo3.exception.HttpCacheMissException
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpInterceptorChain
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
    mockServer.enqueue(MockResponse(statusCode = 200, body = "success"))

    runBlocking {
      val request = HttpRequest.Builder(
          method = HttpMethod.Get,
          url = mockServer.url(),
      ).build()

      var response = interceptor.intercept(request, chain)
      assertEquals("success", response.body?.readUtf8())

      // 2nd request should hit the cache
      response = interceptor.intercept(
          request.newBuilder()
              .addHeader(CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, CachingHttpInterceptor.CACHE_ONLY)
              .build(),
          chain
      )
      assertEquals("success", response.body?.readUtf8())
      assertEquals("true", response.headers.valueOf(CachingHttpInterceptor.FROM_CACHE))
    }
  }

  @Test
  fun failureResponsesAreNotCached() {
    mockServer.enqueue(MockResponse(statusCode = 500, body = "error"))

    runBlocking {
      val request = HttpRequest.Builder(
          method = HttpMethod.Get,
          url = mockServer.url(),
      ).build()

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
    mockServer.enqueue(MockResponse(statusCode = 200, body = "success"))

    runBlocking {
      val request = HttpRequest.Builder(
          method = HttpMethod.Get,
          url = mockServer.url(),
      ).build()

      // Warm the cache
      var response = interceptor.intercept(request, chain)
      assertEquals("success", response.body?.readUtf8())

      // 2nd request should hit the cache
      response = interceptor.intercept(
          request.newBuilder()
              .addHeader(CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, CachingHttpInterceptor.CACHE_ONLY)
              .build(),
          chain
      )
      assertEquals("success", response.body?.readUtf8())

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
      mockServer.enqueue(MockResponse(statusCode = 200, body = "success"))
    }
    val jobs = mutableListOf<Job>()
    runBlocking {
      repeat(concurrency) {
        jobs += launch(Dispatchers.IO) {
          val request = HttpRequest.Builder(
              method = HttpMethod.Get,
              url = mockServer.url(),
          ).build()

          val response = interceptor.intercept(request, chain)
          assertEquals("success", response.body?.readUtf8())
        }
      }
      jobs.forEach { it.join() }
    }
  }
}

private class TestHttpInterceptorChain : HttpInterceptorChain {
  val engine = DefaultHttpEngine()

  override suspend fun proceed(request: HttpRequest): HttpResponse {
    return engine.execute(request)
  }
}
