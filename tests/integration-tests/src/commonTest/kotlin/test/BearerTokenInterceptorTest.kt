package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.BearerTokenInterceptor
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.TestTokenProvider
import com.apollographql.apollo3.testing.runTest
import testFixtureToUtf8
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ApolloExperimental::class)
class BearerTokenInterceptorTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var tokenProvider: TestTokenProvider

  private var token1 = "token1"
  private var token2 = "token2"

  private fun setUp() {
    tokenProvider = TestTokenProvider(token1, token2)
    mockServer = MockServer()
    mockServer.enqueue(MockResponse(statusCode = 401))
    mockServer.enqueue(testFixtureToUtf8("HeroNameResponse.json"))
  }

  private suspend fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun succeedsWithInterceptor() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = ApolloClient.Builder()
        .networkTransport(
            HttpNetworkTransport.Builder()
                .serverUrl(mockServer.url())
                .interceptors(listOf(BearerTokenInterceptor(tokenProvider)))
                .build()
        )
        .build()

    val response = apolloClient.query(HeroNameQuery()).execute()
    assertEquals("R2-D2", response.data?.hero?.name)

    assertEquals("Bearer $token1", mockServer.takeRequest().headers["Authorization"])
    assertEquals("Bearer $token2", mockServer.takeRequest().headers["Authorization"])
  }

  @Test
  fun failsWithoutInterceptor() = runTest(before = { setUp() }, after = { tearDown() }) {
    apolloClient = ApolloClient.Builder()
        .networkTransport(
            HttpNetworkTransport.Builder().serverUrl(mockServer.url()).build()
        )
        .build()

    try {
      apolloClient.query(HeroNameQuery()).execute()
    } catch (e: ApolloHttpException) {
      assertEquals(401, e.statusCode)
    }
  }
}