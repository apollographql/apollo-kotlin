
import com.apollographql.apollo3.api.DefaultUpload
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.mockserver.headerValueOf
import com.apollographql.apollo3.testing.mockServerTest
import httpheaders.GetRandomQuery
import httpheaders.UploadMutation
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
