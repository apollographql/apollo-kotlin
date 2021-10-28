
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.http.httpCache
import com.apollographql.apollo3.cache.http.isFromHttpCache
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import httpcache.GetRandomQuery
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals

class WithHttpCacheTest {
  /**
   * Make sure `withHttpCache is working as expected`
   */
  @Test
  fun withHttpCacheTest() = runTest {
    val mockResponse = """
    {
      "data": {
        "random": 42
      }
    }
  """.trimIndent()

    val mockServer = MockServer()
    val dir = File("build/httpCache")
    dir.deleteRecursively()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .httpCache(dir, Long.MAX_VALUE)
        .build()
    mockServer.enqueue(mockResponse)

    runBlocking {
      var response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(false, response.isFromHttpCache)

      response = apolloClient.query(GetRandomQuery()).execute()
      assertEquals(42, response.data?.random)
      assertEquals(true, response.isFromHttpCache)
    }
  }
}