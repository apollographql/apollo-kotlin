package test

import checkTestFixture
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.withHttpHeader
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import okio.Buffer
import kotlin.test.Test
import kotlin.test.assertEquals

class HttpRequestComposerTest {

  @Test
  fun requestPostBodyContainsOperationQueryAndVariablesByDefault() {
    val composer = DefaultHttpRequestComposer("/")
    val apolloRequest = ApolloRequest(AllPlanetsQuery())
    val httpRequest = composer.compose(apolloRequest)

    val bodyText = Buffer().also { httpRequest.body?.writeTo(it) }.readUtf8()

    checkTestFixture(bodyText , "IntegrationTest/allPlanets.json")
  }

  @Test
  fun requestHeadersAreForwardedToTheServer() {
    val mockServer = MockServer()
    val apolloClient = ApolloClient(mockServer.url())

    runWithMainLoop {
      kotlin.runCatching {
        // No need to enqueue a successful response, we just want to make sure our headers reached the server
        mockServer.enqueue("error")
        apolloClient.query(ApolloRequest(AllPlanetsQuery()).withHttpHeader("test", "is passing"))
      }

      val response = mockServer.takeRequest()
      assertEquals("is passing", response.headers["test"])
    }
  }
}