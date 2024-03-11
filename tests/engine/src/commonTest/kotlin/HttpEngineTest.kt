import com.apollographql.apollo3.api.http.ByteStringHttpBody
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.valueOf
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.awaitRequest
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class HttpEngineTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private fun tearDown() {
    mockServer.close()
  }

  @Test
  fun errorWithBody() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(
        statusCode = 500,
        string = "Ooops"
    )
    val httpResponse = httpEngine().execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
    assertEquals(500, httpResponse.statusCode)
    assertEquals("Ooops", httpResponse.body?.readUtf8())
  }

  @Test
  fun headers() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(
        MockResponse.Builder()
            .statusCode(204)
            .addHeader("responseHeader1", "responseValue1")
            .addHeader("responseHeader2", "responseValue2")
            .build()
    )
    val httpResponse = httpEngine().execute(
        HttpRequest.Builder(HttpMethod.Get, mockServer.url())
            .addHeader("requestHeader1", "requestValue1")
            .addHeader("requestHeader2", "requestValue2")
            .build()
    )
    val request = mockServer.awaitRequest()
    assertEquals("requestValue1", request.headers["requestHeader1"])
    assertEquals("requestValue2", request.headers["requestHeader2"])

    assertEquals("responseValue1", httpResponse.headers.valueOf("responseHeader1"))
    assertEquals("responseValue2", httpResponse.headers.valueOf("responseHeader2"))
  }

  @Test
  fun post() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(
        MockResponse.Builder()
            .statusCode(204)
            .build()
    )
    httpEngine().execute(
        HttpRequest.Builder(HttpMethod.Post, mockServer.url())
            .body(ByteStringHttpBody("text/plain", "body"))
            .build()
    )
    val request = mockServer.awaitRequest()
    assertEquals("POST", request.method)
    assertEquals("body", request.body.utf8())
    // With ktor we get "text/plain; charset=UTF-8"
    assertTrue(request.headers["Content-Type"]!!.startsWith("text/plain"))
    assertEquals("body".length.toString(), request.headers["Content-Length"])
  }

  @Test
  fun connectTimeout() = runTest(before = { setUp() }, after = { tearDown() }) {
    assertFailsWith<ApolloException> {

      // Not routable IP, results in a timeout (see https://stackoverflow.com/a/904609/15695)
      httpEngine(500).execute(HttpRequest.Builder(HttpMethod.Get, "http://10.0.0.0").build())
    }
  }

  @Test
  fun readTimeout() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(
        MockResponse.Builder()
            .delayMillis(1000)
            .statusCode(200)
            .body("body")
            .build()
    )
    assertFailsWith<ApolloException> {
      httpEngine(500).execute(HttpRequest.Builder(HttpMethod.Get, mockServer.url()).build())
    }
  }
}
