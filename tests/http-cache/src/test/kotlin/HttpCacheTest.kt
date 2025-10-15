import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.api.toResponseJson
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.network.http.DefaultHttpEngine
import com.apollographql.apollo.network.http.HttpNetworkTransport
import com.apollographql.apollo.network.http.isFromHttpCache
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueError
import httpcache.GetRandom2Query
import httpcache.GetRandomQuery
import httpcache.RandomSubscription
import httpcache.SetRandomMutation
import httpcache.builder.Data
import okhttp3.Cache
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

  private fun MockServer.enqueueCachedString(value: String) {
    enqueue(
        MockResponse.Builder()
            .body(value)
            .addHeader("Content-Type", "application/json")
            .addHeader("cache-control", "max-age=100")
            .build()
    )
  }

  private suspend fun before() {
    mockServer = MockServer()
    val dir = File("build/httpCache")
    dir.deleteRecursively()
    apolloClient = ApolloClient.Builder()
        .networkTransport(
            HttpNetworkTransport.Builder()
                .httpRequestComposer(DefaultHttpRequestComposer(serverUrl = mockServer.url(), enablePostCaching = true))
                .httpEngine(
                    DefaultHttpEngine {
                      OkHttpClient.Builder()
                          .cache(Cache(directory = dir, maxSize = Long.MAX_VALUE))
                          .build()
                    }
                )
                .build()
        )
        .build()
  }

  private fun tearDown() {
    apolloClient.close()
    mockServer.close()
  }

  @Test
  fun CacheFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueCachedString(data.toResponseJson())

    var response = apolloClient.query(GetRandomQuery()).execute()
    assertEquals(42, response.data?.random)
    assertEquals(false, response.isFromHttpCache)

    response = apolloClient.query(GetRandomQuery())
        .execute()
    assertEquals(42, response.data?.random)
    assertEquals(true, response.isFromHttpCache)
  }

  @Test
  fun NetworkOnly() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueCachedString(data.toResponseJson())
    mockServer.enqueueError(statusCode = 500)

    val response = apolloClient.query(GetRandomQuery()).execute()
    assertEquals(42, response.data?.random)
    assertEquals(false, response.isFromHttpCache)

    assertNotNull(
        apolloClient.query(GetRandomQuery())
            .addHttpHeader("cache-control", "no-cache")
            .execute()
            .exception
    )
  }

  @Test
  fun CacheOnly() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueCachedString(data.toResponseJson())
    mockServer.enqueueError(statusCode = 500)

    var response = apolloClient.query(GetRandomQuery()).execute()
    assertEquals(42, response.data?.random)
    assertEquals(false, response.isFromHttpCache)

    response = apolloClient.query(GetRandomQuery())
        .addHttpHeader("cache-control", "only-if-cached")
        .execute()
    assertEquals(42, response.data?.random)
    assertEquals(true, response.isFromHttpCache)
  }

  @Test
  fun DifferentQueriesDoNotOverlap() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueCachedString(data.toResponseJson())
    mockServer.enqueueCachedString(data2.toResponseJson())

    val response = apolloClient.query(GetRandomQuery()).execute()
    assertEquals(42, response.data?.random)
    assertEquals(false, response.isFromHttpCache)

    val response2 = apolloClient.query(GetRandom2Query()).execute()
    assertEquals(42, response2.data?.random2)
    assertEquals(false, response2.isFromHttpCache)
  }

  @Test
  fun mutationAreNeverCached() = runTest(before = { before() }, after = { tearDown() }) {
    val mutation = SetRandomMutation()

    repeat(2) {
      mockServer.enqueueCachedString("""
        {
          "data": {
            "setRandom": "42"
          }
        }
      """.trimIndent()
      )
      apolloClient.mutation(mutation)
          .execute()

      // The HTTP request should hit the network
      mockServer.awaitRequest()
    }
  }

  @Test
  fun httpErrorsAreNotCached() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueError(statusCode = 500)
    mockServer.enqueueError(statusCode = 500)

    var response = apolloClient.query(GetRandomQuery()).execute()
    assertEquals(false, response.isFromHttpCache)
    response.exception.apply {
      assertIs<ApolloHttpException>(this)
      assertEquals(500, statusCode)
    }

    response = apolloClient.query(GetRandomQuery())
        .addHttpHeader("cache-control", "only-if-cached")
        .execute()
    assertEquals(false, response.isFromHttpCache)
    response.exception.apply {
      assertIs<ApolloHttpException>(this)
      assertEquals(504, statusCode)
    }
  }


  @Test
  fun CanSetTheDefaultBehaviourAtTheClientLevel() = runTest(before = { before() }, after = { tearDown() }) {
    apolloClient = apolloClient.newBuilder()
        .addHttpHeader("cache-control", "only-if-cached")
        .build()

    mockServer.enqueueCachedString(data.toResponseJson())

    assertNotNull(
        apolloClient.query(GetRandomQuery())
            .addHttpHeader("foo", "bar")
            .execute()
            .exception
    )
  }

  @Test
  fun errorInSubscriptionDoesntRemoveCachedResult() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueueCachedString(data.toResponseJson())
    var response = apolloClient.query(GetRandomQuery()).execute()
    assertEquals(42, response.data?.random)
    assertEquals(false, response.isFromHttpCache)

    mockServer.enqueueCachedString(data.toResponseJson())
    try {
      apolloClient.subscription(RandomSubscription()).execute()
    } catch (_: Exception) {
    }

    response = apolloClient.query(GetRandomQuery()).addHttpHeader("cache-control", "only-if-cached").execute()
    assertEquals(42, response.data?.random)
    assertEquals(true, response.isFromHttpCache)
  }
}
