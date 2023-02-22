import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.http.HttpFetchPolicy
import com.apollographql.apollo3.cache.http.httpCache
import com.apollographql.apollo3.cache.http.httpExpireTimeout
import com.apollographql.apollo3.cache.http.httpFetchPolicy
import com.apollographql.apollo3.cache.http.isFromHttpCache
import com.apollographql.apollo3.exception.ApolloParseException
import com.apollographql.apollo3.exception.HttpCacheMissException
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.okHttpClient
import com.apollographql.apollo3.testing.enqueueData
import com.apollographql.apollo3.testing.internal.runTest
import httpcache.GetRandom2Query
import httpcache.GetRandomQuery
import httpcache.RandomSubscription
import httpcache.SetRandomMutation
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

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

  private suspend fun tearDown() {
    apolloClient.close()
    mockServer.stop()
  }

  @Test
  fun DefaultIsCacheFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueData(data)

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
    mockServer.enqueueData(data)

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
    mockServer.enqueueData(data)
    mockServer.enqueue(statusCode = 500)

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
    mockServer.enqueueData(data)
    mockServer.enqueue(statusCode = 500)

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
    mockServer.enqueueData(data)

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
    mockServer.enqueueData(data)
    mockServer.enqueueData(data2)

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
    mockServer.enqueue(statusCode = 200)
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

  @Test
  fun mutationAreNotCachedByDefault() = runTest(before = { before() }, after = { tearDown() }) {
    val mutation = SetRandomMutation()

    repeat(2) {
      mockServer.enqueue("""
        {
          "data": {
            "setRandom": "42"
          }
        }
      """.trimIndent())
      apolloClient.mutation(mutation).execute()

      /**
       * The HTTP request should hit the network twice
       */
      mockServer.takeRequest()
    }
  }

  @Test
  fun incompleteJsonIsNotCached() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue("""{"data":""")
    assertIs<ApolloParseException>(
        apolloClient.query(GetRandomQuery()).execute().exception
    )
    // Should not have been cached
    assertIs<HttpCacheMissException>(
        apolloClient.query(GetRandomQuery()).httpFetchPolicy(HttpFetchPolicy.CacheOnly).execute().exception
    )
  }

  @Test
  fun responseWithGraphQLErrorIsNotCached() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue("""
        {
          "data": {
            "random": 42
          },
          "errors": [ { "message": "GraphQL error" } ]
        }
      """)
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

    mockServer.enqueueData(data)

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
      mockServer.enqueueData(data)
      var response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      mockServer.enqueueData(data)
      try {
        apolloClient.subscription(RandomSubscription()).execute()
      } catch (ignored: Exception) {
      }

      response = apolloClient.query(GetRandomQuery()).httpFetchPolicy(HttpFetchPolicy.CacheOnly).execute()
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)
    }
  }

}
