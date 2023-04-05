package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import com.example.GetRandomQuery
import org.junit.Test
import kotlin.test.assertEquals

class HeadersTest {

  private val operation = GetRandomQuery()
  private val data = GetRandomQuery.Data { }

  @Test
  fun addHeaderUsingAddHttpHeader() = runTest {
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
  fun addHeaderUsingHttpHeaders() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueue(operation, data)
    apolloClient.query(GetRandomQuery()).httpHeaders(listOf(HttpHeader("requestKey", "requestValue"))).execute()

    mockServer.takeRequest().also {
      assertEquals("clientValue", it.headers.get("clientKey"))
      assertEquals("requestValue", it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.stop()
  }

  @Test
  fun replaceHeadersUsingAddHttpHeader() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueue(operation, data)
    apolloClient.query(GetRandomQuery()).addHttpHeader("requestKey", "requestValue").ignoreApolloClientHttpHeaders(true).execute()

    mockServer.takeRequest().also {
      assertEquals(null, it.headers.get("clientKey"))
      assertEquals("requestValue", it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.stop()
  }

  @Test
  fun replaceHeadersUsingHttpHeaders() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueue(operation, data)
    apolloClient.query(GetRandomQuery()).httpHeaders(listOf(HttpHeader("requestKey", "requestValue"))).ignoreApolloClientHttpHeaders(true).execute()

    mockServer.takeRequest().also {
      assertEquals(null, it.headers.get("clientKey"))
      assertEquals("requestValue", it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.stop()
  }

  @Test
  fun replaceAllHeaders() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueue(operation, data)
    apolloClient.query(GetRandomQuery()).ignoreApolloClientHttpHeaders(true).execute()

    mockServer.takeRequest().also {
      assertEquals(null, it.headers.get("clientKey"))
    }

    apolloClient.close()
    mockServer.stop()
  }
}
