import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.cache.http.CachingHttpEngine
import com.apollographql.apollo3.cache.http.HttpFetchPolicy
import com.apollographql.apollo3.cache.http.isFromHttpCache
import com.apollographql.apollo3.cache.http.withHttpExpireTimeout
import com.apollographql.apollo3.cache.http.withHttpFetchPolicy
import com.apollographql.apollo3.exception.HttpCacheMissException
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import rxjava.GetRandom2Query
import rxjava.GetRandomQuery
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
    apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            httpRequestComposer = DefaultHttpRequestComposer(mockServer.url()),
            engine = CachingHttpEngine(dir, Long.MAX_VALUE)
        )
    )
  }

  private suspend fun tearDown() {
    apolloClient.dispose()
    mockServer.stop()
  }

  @Test
  fun CacheFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)

    runBlocking {
      var response = apolloClient.query(GetRandomQuery())
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      response = apolloClient.query(GetRandomQuery())
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)
    }
  }

  @Test
  fun NetworkOnly() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)
    mockServer.enqueue(MockResponse(statusCode = 500))

    runBlocking {
      val response = apolloClient.query(GetRandomQuery())
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      val request = ApolloRequest(GetRandomQuery()).withHttpFetchPolicy(HttpFetchPolicy.NetworkOnly)
      assertFails {
        apolloClient.query(request)
      }
    }
  }

  @Test
  fun NetworkFirst() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)
    mockServer.enqueue(MockResponse(statusCode = 500))

    runBlocking {
      var response = apolloClient.query(GetRandomQuery())
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      val request = ApolloRequest(GetRandomQuery()).withHttpFetchPolicy(HttpFetchPolicy.NetworkFirst)
      response = apolloClient.query(request)
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)
    }
  }

  @Test
  fun Timeout() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)

    runBlocking {
      var response = apolloClient.query(GetRandomQuery())
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      response = apolloClient.query(ApolloRequest(GetRandomQuery()).withHttpExpireTimeout(500))
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)

      delay(1000)
      assertFailsWith(HttpCacheMissException::class) {
        apolloClient.query(ApolloRequest(GetRandomQuery())
            .withHttpExpireTimeout(500)
            .withHttpFetchPolicy(HttpFetchPolicy.CacheOnly)
        )
      }
    }
  }

  @Test
  fun DifferentQueriesDoNotOverlap() = runTest(before = { before() }, after = { tearDown() }) {
    mockServer.enqueue(response)
    mockServer.enqueue(response2)

    runBlocking {
      val response = apolloClient.query(GetRandomQuery())
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      val response2 = apolloClient.query(GetRandom2Query())
      assertEquals(42, response2.data?.random2)
      assertEquals(false, response2.isFromHttpCache)
    }
  }
}

