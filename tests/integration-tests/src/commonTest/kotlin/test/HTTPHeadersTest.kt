package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.composeJsonResponse
import com.apollographql.apollo.api.http.valueOf
import com.apollographql.apollo.api.json.buildJsonString
import com.apollographql.apollo.integration.normalizer.HeroNameQuery
import com.apollographql.mockserver.MockResponse
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.awaitRequest
import com.apollographql.mockserver.enqueueString
import com.apollographql.apollo.network.http.HttpInfo
import com.apollographql.apollo.testing.internal.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class HTTPHeadersTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  private suspend fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient.Builder().serverUrl(mockServer.url()).build()
  }

  private suspend fun tearDown() {
    mockServer.close()
  }

  @Test
  fun makeSureHeadersAreSet() = runTest(before = { setUp() }, after = { tearDown() }) {
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))

    mockServer.enqueueString(query.composeJsonResponse(data))

    val response = apolloClient.query(query).execute()

    assertNotNull(response.data)

    val recordedRequest = mockServer.awaitRequest()
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
        MockResponse.Builder()
            .body(json)
            .addHeader("Header1", "Value1")
            .addHeader("Header2", "Value2")
            .build()
    )

    val response = apolloClient.query(query).execute()

    assertEquals(response.executionContext[HttpInfo]?.headers?.valueOf("Header1"), "Value1")
    assertEquals(response.executionContext[HttpInfo]?.headers?.valueOf("Header2"), "Value2")
  }
}
