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
    mockServer.close()
  }

  @Test
  fun replaceHeaders() = runTest {
    val mockServer = MockServer()
    val apolloClient = ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .addHttpHeader("clientKey", "clientValue")
        .build()

    mockServer.enqueue(operation, data)
    apolloClient.query(GetRandomQuery())
        .httpHeaders(emptyList())
        .ignoreApolloClientHttpHeaders(true)
        .execute()

    mockServer.takeRequest().also {
      assertEquals(null, it.headers.get("clientKey"))
      assertEquals(null, it.headers.get("requestKey"))
    }

    apolloClient.close()
    mockServer.close()
  }

  @Test
  fun addHeadersIsSurfacedInHeaders() = runTest {
    val apolloClient = ApolloClient.Builder().serverUrl("").build()
    val apolloCall = apolloClient.query(GetRandomQuery()).addHttpHeader("requestKey", "requestValue")
    assertTrue(apolloCall.httpHeaders!!.any { it.name == "requestKey" && it.value == "requestValue" })
  }
}
