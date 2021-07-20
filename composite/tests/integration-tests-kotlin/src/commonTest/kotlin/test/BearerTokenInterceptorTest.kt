package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.network.http.BearerTokenInterceptor
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.testing.TestTokenProvider
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import readResource
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BearerTokenInterceptorTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient
  private lateinit var tokenProvider: TestTokenProvider

  private var token1 = "token1"
  private var token2 = "token2"

  @BeforeTest
  fun setUp() {
    tokenProvider = TestTokenProvider(token1, token2)
    mockServer = MockServer()
    mockServer.enqueue(MockResponse(statusCode = 401))
    mockServer.enqueue(readResource("HeroNameResponse.json"))
  }

  @AfterTest
  fun tearDown() {
    mockServer.stop()
  }

  @Test
  fun succeedsWithInterceptor() = runTest {
    apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            serverUrl = mockServer.url(),
            interceptors = listOf(BearerTokenInterceptor(tokenProvider))
        )
    )

    val response = apolloClient.query(HeroNameQuery())
    assertEquals("R2-D2", response.data?.hero?.name)

    assertEquals("Bearer $token1", mockServer.takeRequest().headers["Authorization"])
    assertEquals("Bearer $token2", mockServer.takeRequest().headers["Authorization"])
  }

  @Test
  fun failsWithoutInterceptor() = runTest {
    apolloClient = ApolloClient(
        networkTransport = HttpNetworkTransport(
            serverUrl = mockServer.url(),
        )
    )

    try {
      apolloClient.query(HeroNameQuery())
    } catch (e: ApolloHttpException) {
      assertEquals(401, e.statusCode)
    }
  }
}