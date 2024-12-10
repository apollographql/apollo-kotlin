
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.toResponseJson
import com.apollographql.apollo.cache.http.ApolloHttpCache
import com.apollographql.apollo.cache.http.DiskLruHttpCache
import com.apollographql.apollo.cache.http.HttpFetchPolicy
import com.apollographql.apollo.cache.http.httpCache
import com.apollographql.apollo.cache.http.httpExpireTimeout
import com.apollographql.apollo.cache.http.httpFetchPolicy
import com.apollographql.apollo.cache.http.isFromHttpCache
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.HttpCacheMissException
import com.apollographql.apollo.exception.JsonEncodingException
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueError
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.network.okHttpClient
import com.apollographql.apollo.testing.internal.runTest
import httpcache.GetRandom2Query
import httpcache.GetRandomQuery
import httpcache.RandomSubscription
import httpcache.SetRandomMutation
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okio.FileSystem
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class HttpCacheTest {
  lateinit var mockServer: MockServer
  lateinit var apolloClient: ApolloClient

  private val data = GetRandomQuery.Data {
    random = 42
  }

  private val data2 = GetRandom2Query.Data {
    random2 = 42
  }

  private suspend fun before() {
    mockServer = MockServer()
    val dir = File("build/httpCache")
    dir.deleteRecursively()
    apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpCache(dir, Long.MAX_VALUE)
        .build()
  }

  private fun tearDown() {
    apolloClient.close()
    mockServer.close()
  }

  @Test
  fun DefaultIsCacheFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString(data.toResponseJson())

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
  fun CacheFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString(data.toResponseJson())

    runBlocking {
      var response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      response = apolloClient.query(GetRandomQuery())
          .httpFetchPolicy(HttpFetchPolicy.CacheFirst)
          .execute()
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)
    }
  }

  @Test
  fun NetworkOnly() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString(data.toResponseJson())
    mockServer.enqueueError(statusCode = 500)

    runBlocking {
      val response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      assertNotNull(
          apolloClient.query(GetRandomQuery())
              .httpFetchPolicy(HttpFetchPolicy.NetworkOnly)
              .execute()
              .exception
      )
    }
  }

  @Test
  fun NetworkFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString(data.toResponseJson())
    mockServer.enqueueError(statusCode = 500)

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
    mockServer.enqueueString(data.toResponseJson())

    runBlocking {
      var response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      response = apolloClient.query(GetRandomQuery()).httpExpireTimeout(500).execute()
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)

      delay(1000)
      assertIs<HttpCacheMissException>(
          apolloClient.query(GetRandomQuery())
              .httpExpireTimeout(500)
              .httpFetchPolicy(HttpFetchPolicy.CacheOnly)
              .execute()
              .exception
      )
    }
  }

  @Test
  fun DifferentQueriesDoNotOverlap() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString(data.toResponseJson())
    mockServer.enqueueString(data2.toResponseJson())

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
    mockServer.enqueueString("")
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

    assertEquals("Test-Value", mockServer.awaitRequest().headers["Test-Header"])
  }

  @Test
  fun mutationAreNeverCached() = runTest(before = { before() }, after = { tearDown() }) {
    val mutation = SetRandomMutation()

    repeat(2) {
      mockServer.enqueueString("""
        {
          "data": {
            "setRandom": "42"
          }
        }
      """.trimIndent()
      )
      apolloClient.mutation(mutation)
          .httpFetchPolicy(HttpFetchPolicy.CacheOnly)
          .execute()

      // The HTTP request should hit the network
      mockServer.awaitRequest()
    }
  }

  /**
   * Whether an incomplete Json is an IO error is still an open question:
   * - [ResponseParser] considers yes (and throws an ApolloNetworkException)
   * - [ProxySource] considers no (and doesn't abort)
   *
   * This isn't great and will need to be revisited if that ever becomes a bigger problem
   */
  @Test
  fun incompleteJsonTriggersNetworkException() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString("""{"data":""")
    apolloClient.query(GetRandomQuery()).execute().apply {
      assertIs<ApolloNetworkException>(exception)
    }

    apolloClient.query(GetRandomQuery()).httpFetchPolicy(HttpFetchPolicy.CacheOnly).execute().apply {
      /**
       * Because there's a disagreement between ProxySource and HttpCacheApolloInterceptor, the value is stored in the
       * HTTP cache and is replayed here
       */
      assertIs<ApolloNetworkException>(exception)
    }
  }

  @Test
  fun malformedJsonIsNotCached() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString("""{"data":}""")
    apolloClient.query(GetRandomQuery()).execute().exception.apply {
      assertIs<JsonEncodingException>(this)
    }
    // Should not have been cached
    apolloClient.query(GetRandomQuery()).httpFetchPolicy(HttpFetchPolicy.CacheOnly).execute().apply {
      assertIs<HttpCacheMissException>(exception)
    }
  }

  @Test
  fun ioErrorDoesNotRemoveData() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString("""
        {
          "data": {
            "random": "42"
          }
        }
      """.trimIndent())

    // Warm the cache
    apolloClient.query(GetRandomQuery()).execute().apply {
      assertEquals(42, data?.random)
      assertFalse(isFromHttpCache)
    }

    // Go offline
    mockServer.close()
    apolloClient.query(GetRandomQuery()).httpFetchPolicy(HttpFetchPolicy.NetworkOnly).execute().apply {
      assertIs<ApolloNetworkException>(exception)
    }

    // The data is still there
    apolloClient.query(GetRandomQuery()).execute().apply {
      assertEquals(42, data?.random)
      assertTrue(isFromHttpCache)
    }
  }


  @Test
  fun responseWithGraphQLErrorIsNotCached() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueString("""
        {
          "data": {
            "random": 42
          },
          "errors": [ { "message": "GraphQL error" } ]
        }
      """
    )
    apolloClient.query(GetRandomQuery()).execute()
    // Should not have been cached
    assertIs<HttpCacheMissException>(
        apolloClient.query(GetRandomQuery()).httpFetchPolicy(HttpFetchPolicy.CacheOnly).execute().exception
    )
  }

  @Test
  fun CanSetTheDefaultBehaviourAtTheClientLevel() = runTest(before = { before() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder()
        .httpFetchPolicy(HttpFetchPolicy.CacheOnly)
        .build()

    mockServer.enqueueString(data.toResponseJson())

    assertNotNull(
        apolloClient.query(GetRandomQuery())
            .addHttpHeader("foo", "bar")
            .execute()
            .exception
    )
  }

  @Test
  fun errorInSubscriptionDoesntRemoveCachedResult() = runTest(before = { before() }, after = { tearDown() }) {
    runBlocking {
      mockServer.enqueueString(data.toResponseJson())
      var response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      mockServer.enqueueString(data.toResponseJson())
      try {
        apolloClient.subscription(RandomSubscription()).execute()
      } catch (ignored: Exception) {
      }

      response = apolloClient.query(GetRandomQuery()).httpFetchPolicy(HttpFetchPolicy.CacheOnly).execute()
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)
    }
  }

  @Test
  fun httpCacheCleansPreviousInterceptor() = runTest {
    mockServer = MockServer()
    val httpCache1 = CountingApolloHttpCache()
    mockServer.enqueueString(data.toResponseJson())
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpCache(httpCache1)
        .build()
    apolloClient.query(GetRandomQuery()).execute()
    assertEquals(1, httpCache1.writes)

    val httpCache2 = CountingApolloHttpCache()
    val apolloClient2 = apolloClient.newBuilder()
        .httpCache(httpCache2)
        .build()
    mockServer.enqueueString(data.toResponseJson())
    apolloClient2.query(GetRandomQuery()).execute()
    assertEquals(1, httpCache1.writes)
    assertEquals(1, httpCache2.writes)
  }
}

private class CountingApolloHttpCache : ApolloHttpCache {
  private val wrapped = run  {
    val dir = File("build/httpCache")
    dir.deleteRecursively()
    DiskLruHttpCache(FileSystem.SYSTEM, dir, Long.MAX_VALUE)
  }
  var writes = 0
  override fun write(response: HttpResponse, cacheKey: String): HttpResponse {
    writes++
    return wrapped.write(response, cacheKey)
  }

  override fun read(cacheKey: String): HttpResponse {
    return wrapped.read(cacheKey)
  }

  override fun clearAll() {}

  override fun remove(cacheKey: String) {}
}
