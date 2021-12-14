import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.http.HttpFetchPolicy
import com.apollographql.apollo3.cache.http.httpCache
import com.apollographql.apollo3.cache.http.httpExpireTimeout
import com.apollographql.apollo3.cache.http.httpFetchPolicy
import com.apollographql.apollo3.cache.http.isFromHttpCache
import com.apollographql.apollo3.exception.HttpCacheMissException
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.testing.runTest
import httpcache.GetRandom2Query
import httpcache.GetRandomQuery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith

class HttpCacheTest {
  lateinit var mockServer: MockServer
  lateinit var apolloClient: ApolloClient

  private val response = """
    {
      "data": {
        "random": 42
      }
    }
  """.trimIndent()

  private val response2 = """
    {
      "data": {
        "random2": 42
      }
    }
  """.trimIndent()

  private suspend fun before() {
    mockServer = MockServer()
    val dir = File("build/httpCache")
    dir.deleteRecursively()
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpCache(dir, Long.MAX_VALUE)
        .build()
  }

  private suspend fun tearDown() {
    apolloClient.dispose()
    mockServer.stop()
  }

  @Test
  fun CacheFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)

    runBlocking {
      var response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)
    }
  }

  @Test
  fun NetworkOnly() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)
    mockServer.enqueue(MockResponse(statusCode = 500))

    runBlocking {
      val response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      assertFails {
        apolloClient.query(GetRandomQuery())
            .httpFetchPolicy(HttpFetchPolicy.NetworkOnly)
            .execute()
      }
    }
  }

  @Test
  fun NetworkFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)
    mockServer.enqueue(MockResponse(statusCode = 500))

    runBlocking {
      var response = apolloClient.query(GetRandomQuery())
          .execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      response = apolloClient.query(GetRandomQuery())
          .httpFetchPolicy(HttpFetchPolicy.NetworkFirst)
          .execute()
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)
    }
  }

  @Test
  fun Timeout() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)

    runBlocking {
      var response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      response = apolloClient.query(GetRandomQuery()).httpExpireTimeout(500).execute()
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)

      delay(1000)
      assertFailsWith(HttpCacheMissException::class) {
        apolloClient.query(GetRandomQuery())
            .httpExpireTimeout(500)
            .httpFetchPolicy(HttpFetchPolicy.CacheOnly)
            .execute()
      }
    }
  }

  @Test
  fun DifferentQueriesDoNotOverlap() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)
    mockServer.enqueue(response2)

    runBlocking {
      val response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      val response2 = apolloClient.query(GetRandom2Query()).execute()
      assertEquals(42, response2.data?.random2)
      assertEquals(false, response2.isFromHttpCache)
    }
  }

  @Test
  fun HttpCacheDoesNotOverrideOkHttpClient() = runTest {
    val interceptor = Interceptor {
      it.proceed(it.request().newBuilder().header("Test-Header", "Test-Value").build())
    }
    val okHttpClient = OkHttpClient.Builder().addInterceptor(interceptor).build()

    val mockServer = MockServer()

    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .okHttpClient(okHttpClient)
        .httpCache(File("build/httpCache"), Long.MAX_VALUE)
        .build()

    kotlin.runCatching {
      apolloClient.query(GetRandomQuery())
          .httpFetchPolicy(HttpFetchPolicy.NetworkOnly)
          .execute()
    }

    assertEquals("Test-Value", mockServer.takeRequest().headers["Test-Header"])
  }
}

