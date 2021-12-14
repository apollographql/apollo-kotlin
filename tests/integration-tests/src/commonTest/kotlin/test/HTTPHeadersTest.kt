package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.composeJsonResponse
import com.apollographql.apollo3.api.http.valueOf
import com.apollographql.apollo3.api.json.buildJsonString
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.network.http.HttpInfo
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

@OptIn(ApolloExperimental::class)
class HTTPHeadersTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun makeSureHeadersAreSet() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    mockServer.enqueue(query, data)

    val response = apolloClient.query(query).execute()

    assertNotNull(response.data)

    val recordedRequest = mockServer.takeRequest()
    assertEquals("POST", recordedRequest.method)
    assertNotEquals(null, recordedRequest.headers["Content-Length"])
    assertNotEquals("0", recordedRequest.headers["Content-Length"])
  }

  @Test
  fun headersCanBeReadInResponseExecutionContext() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    val json = buildJsonString {
      query.composeJsonResponse(this, data)
    }

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

    val response = apolloClient.query(query).execute()

    assertEquals(response.executionContext[HttpInfo]?.headers?.valueOf("Header1"), "Value1")
    assertEquals(response.executionContext[HttpInfo]?.headers?.valueOf("Header2"), "Value2")
  }
}