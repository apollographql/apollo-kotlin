package test

import checkTestFixture
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo.integration.httpcache.AllPlanetsQuery
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.testing.internal.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRequestComposerTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.close()
  }

  @Test
  fun requestPostBodyContainsOperationQueryAndVariablesByDefault() {
    val composer = DefaultHttpRequestComposer("/")
    val apolloRequest = ApolloRequest.Builder(AllPlanetsQuery()).build()
    val httpRequest = composer.compose(apolloRequest)

    val bodyText = Buffer().also { httpRequest.body?.writeTo(it) }.readUtf8()

    checkTestFixture(bodyText, "allPlanets.json")
  }

  @Test
  fun requestHeadersAreForwardedToTheServer() = runTest(before = { setUp() }, after = { tearDown() }) {
    val apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()

    kotlin.runCatching {
      // No need to enqueue a successful response, we just want to make sure our headers reached the server
      mockServer.enqueueString("error")
      apolloClient.query(AllPlanetsQuery()).addHttpHeader("test", "is passing").execute()
    }

    val response = mockServer.awaitRequest()
    assertEquals("is passing", response.headers["test"])
  }
}
