package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import com.example.GetRandomQuery
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class HeadersTest {

  private val operation = GetRandomQuery()
  private val data = GetRandomQuery.Data { }

  @Test
  fun addHeader() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueue(operation, data)
    apolloClient.query(GetRandomQuery()).addHttpHeader("requestKey", "requestValue").execute()

    mockServer.takeRequest().also {
      assertEquals("clientValue", it.headers.get("clientKey"))
      assertEquals("requestValue", it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.stop()
  }

  @Test
  fun replaceHeaders() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueue(operation, data)
    apolloClient.query(GetRandomQuery()).httpHeaders(emptyList()).execute()

    mockServer.takeRequest().also {
      assertEquals(null, it.headers.get("clientKey"))
      assertEquals(null, it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.stop()
  }

  @Test
  fun addAndReplaceHeadersThrows() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    try {
      apolloClient.query(GetRandomQuery())
          .httpHeaders(listOf(HttpHeader("requestKey", "requestValue")))
          .addHttpHeader("requestKey", "requestValue")
          .execute()

      fail("an exception was expected")
    } catch (e: Exception) {
      assertTrue(e.message!!.contains("it is an error to call both .headers() and .addHeader()"))
    }

    apolloClient.close()
    mockServer.stop()
  }
}
