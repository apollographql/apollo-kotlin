package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.runTest
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ApolloExperimental::class)
class ExceptionsTest {
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
  fun whenQueryAndMalformedNetworkResponseAssertException() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue("malformed")

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery()).execute()
    }

    assertTrue(result.exceptionOrNull() != null)
  }

  @Test
  fun whenHttpErrorAssertExecuteFails() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(MockResponse(statusCode = 404))

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery()).execute()
    }

    val exception = result.exceptionOrNull()
    assertTrue(exception is ApolloHttpException)
    assertEquals(404, exception.statusCode)
  }

  @Test
  fun whenNetworkErrorAssertApolloNetworkException() = runTest(before = { setUp() }) {
    mockServer.stop()

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery()).execute()
    }

    val exception = result.exceptionOrNull()
    assertTrue(exception is ApolloNetworkException)
  }

  @Test
  fun WhenQueryAndMalformedNetworkResponseAssertSuccessAfterRetry() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue("")
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    val response = apolloClient
        .query(query)
        .toFlow()
        .retryWhen { _, attempt -> attempt == 0L }
        .single()

    assertEquals(data, response.data)
  }
}
