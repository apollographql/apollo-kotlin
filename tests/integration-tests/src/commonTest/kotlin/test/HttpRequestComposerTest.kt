package test

import checkTestFixture
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
class HttpRequestComposerTest {
  private lateinit var mockServer: MockServer

  private fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
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
      mockServer.enqueue("error")
      apolloClient.query(AllPlanetsQuery()).addHttpHeader("test", "is passing").execute()
    }

    val response = mockServer.takeRequest()
    assertEquals("is passing", response.headers["test"])
  }
}
