
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.DefaultUpload
import com.apollographql.apollo.api.Optional
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import com.apollographql.mockserver.headerValueOf
import com.apollographql.apollo.testing.internal.runTest
import httpheaders.GetRandomQuery
import httpheaders.UploadMutation
import kotlinx.coroutines.CoroutineScope
import okio.use
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpHeadersTest {
  @Test
  fun postRequestsDoNotSendPreflightHeader() = mockServerTest {

    mockServer.enqueueString("")
    apolloClient.query(GetRandomQuery()).execute()

    mockServer.takeRequest().apply {
      assertEquals(null, headers.headerValueOf("apollo-require-preflight"))
    }
  }

  @Test
  fun getRequestsSendPreflightHeader() = mockServerTest(
      clientBuilder = { autoPersistedQueries() }
  ){

    mockServer.enqueueString("")
    apolloClient.query(GetRandomQuery()).enableAutoPersistedQueries(true).execute()

    mockServer.takeRequest().apply {
      assertEquals("GET", method)
      assertEquals("true", headers.headerValueOf("apollo-require-preflight"))
    }
  }

  @Test
  fun uploadRequestsSendPreflightHeader() = mockServerTest {

    mockServer.enqueueString("")
    val upload = DefaultUpload.Builder()
        .content("hello")
        .contentLength(5)
        .contentType("text/plain")
        .fileName("hello")
        .build()

    apolloClient.mutation(UploadMutation(Optional.present(upload))).execute()

    mockServer.takeRequest().apply {
      assertEquals("POST", method)
      assertEquals("true", headers.headerValueOf("apollo-require-preflight"))
    }
  }
}

class MockServerTest(val mockServer: MockServer, val apolloClient: ApolloClient, val scope: CoroutineScope)

fun mockServerTest(
    clientBuilder: ApolloClient.Builder.() -> Unit = {},
    block: suspend MockServerTest.() -> Unit
) = runTest {
  MockServer().use { mockServer ->
    ApolloClient.Builder()
        .serverUrl(mockServer.url())
        .apply(clientBuilder)
        .build()
        .use {apolloClient ->
          MockServerTest(mockServer, apolloClient, this).block()
        }
  }
}
