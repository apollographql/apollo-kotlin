package test

import checkTestFixture
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.withHttpHeader
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runTest
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRequestComposerTest {
  private lateinit var mockServer: MockServer

  private suspend fun setUp() {
    mockServer = MockServer()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun requestPostBodyContainsOperationQueryAndVariablesByDefault() {
    val composer = DefaultHttpRequestComposer("/")
    val apolloRequest = ApolloRequest(AllPlanetsQuery())
    val httpRequest = composer.compose(apolloRequest)

    val bodyText = Buffer().also { httpRequest.body?.writeTo(it) }.readUtf8()

    checkTestFixture(bodyText, "IntegrationTest/allPlanets.json")
  }

  @Test
  fun requestHeadersAreForwardedToTheServer() = runTest(before = { setUp() }, after = { tearDown() }) {
    val apolloClient = ApolloClient(mockServer.url())

    kotlin.runCatching {
      // No need to enqueue a successful response, we just want to make sure our headers reached the server
      mockServer.enqueue("error")
      apolloClient.query(ApolloRequest(AllPlanetsQuery()).withHttpHeader("test", "is passing"))
    }

    val response = mockServer.takeRequest()
    assertEquals("is passing", response.headers["test"])
  }
}