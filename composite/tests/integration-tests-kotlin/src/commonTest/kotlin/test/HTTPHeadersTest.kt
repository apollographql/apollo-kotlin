package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.integration.httpcache.AllPlanetsQuery
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.HttpResponseInfo
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class HTTPHeadersTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url())
  }

  @Test
  fun makeSureHeadersAreSet() {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

    mockServer.enqueue(query, data)

    runWithMainLoop {
      val response = apolloClient.query(query)

      assertNotNull(response.data)

      val recordedRequest = mockServer.takeRequest()
      assertEquals("POST", recordedRequest.method)
      assertNotEquals(null, recordedRequest.headers["Content-Length"])
      assertNotEquals("0", recordedRequest.headers["Content-Length"])
    }
  }

  @Test
  fun headersCanBeReadInResponseExecutionContext() {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))

    val json = query.composeJsonResponse(data)

    mockServer.enqueue(
        MockResponse(
            statusCode = 200,
            body = json,
            headers = mapOf(
                "Header1" to "Value1",
                "Header2" to "Value2"
            )
        )
    )

    runWithMainLoop {
      val response = apolloClient.query(query)

      assertEquals(response.executionContext[HttpResponseInfo]?.headers?.get("Header1"), "Value1")
      assertEquals(response.executionContext[HttpResponseInfo]?.headers?.get("Header2"), "Value2")
    }
  }
}