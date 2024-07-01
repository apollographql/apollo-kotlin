package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.composeJsonResponse
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
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

    mockServer.enqueueString(operation.composeJsonResponse(data))
    apolloClient.query(GetRandomQuery()).addHttpHeader("requestKey", "requestValue").execute()

    mockServer.awaitRequest().also {
      assertEquals("clientValue", it.headers.get("clientKey"))
      assertEquals("requestValue", it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.close()
  }

  @Test
  fun addHeaderUsingHttpHeaders() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueueString(operation.composeJsonResponse(data))
    apolloClient.query(GetRandomQuery()).httpHeaders(listOf(HttpHeader("requestKey", "requestValue"))).execute()

    mockServer.awaitRequest().also {
      assertEquals("clientValue", it.headers.get("clientKey"))
      assertEquals("requestValue", it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.close()
  }

  @Test
  fun replaceHeadersUsingAddHttpHeader() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueueString(operation.composeJsonResponse(data))
    apolloClient.query(GetRandomQuery()).addHttpHeader("requestKey", "requestValue").ignoreApolloClientHttpHeaders(true).execute()

    mockServer.awaitRequest().also {
      assertEquals(null, it.headers.get("clientKey"))
      assertEquals("requestValue", it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.close()
  }

  @Test
  fun replaceHeadersUsingHttpHeaders() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueueString(operation.composeJsonResponse(data))
    apolloClient.query(GetRandomQuery()).httpHeaders(listOf(HttpHeader("requestKey", "requestValue"))).ignoreApolloClientHttpHeaders(true).execute()

    mockServer.awaitRequest().also {
      assertEquals(null, it.headers.get("clientKey"))
      assertEquals("requestValue", it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.close()
  }

  @Test
  fun replaceAllHeaders() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueueString(operation.composeJsonResponse(data))
    apolloClient.query(GetRandomQuery()).ignoreApolloClientHttpHeaders(true).execute()

    mockServer.awaitRequest().also {
      assertEquals(null, it.headers.get("clientKey"))
    }

    apolloClient.close()
    mockServer.close()
  }
}
