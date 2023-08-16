package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.flow.toList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    val response = apolloClient.query(HeroNameQuery()).execute()
    assertTrue(response.exception != null)
  }

  @Test
  fun whenHttpErrorAssertExecuteFails() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue(statusCode = 404)

    val response = apolloClient.query(HeroNameQuery()).execute()
    val exception = response.exception
    assertTrue(exception is ApolloHttpException)
    assertEquals(404, exception.statusCode)
  }

  @Test
  fun whenNetworkErrorAssertApolloNetworkException() = runTest(before = { setUp() }) {
    mockServer.stop()

    val response = apolloClient.query(HeroNameQuery()).execute()
    assertTrue(response.exception is ApolloNetworkException)
  }

  @Test
  @Suppress("DEPRECATION")
  fun toFlowThrows() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueue("malformed")

    val throwingClient = apolloClient.newBuilder().useV3ExceptionHandling(true).build()
    var result = kotlin.runCatching {
      throwingClient.query(HeroNameQuery()).toFlow().toList()
    }
    assertNotNull(result.exceptionOrNull())
  }
}
