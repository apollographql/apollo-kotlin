package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.mockserver.enqueueString
import com.apollographql.apollo3.testing.enqueue
import com.apollographql.apollo3.testing.internal.runTest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExceptionsTest {
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
  fun whenQueryAndMalformedNetworkResponseAssertException() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString("malformed")

    assertTrue( apolloClient.query(HeroNameQuery()).execute().exception != null)
  }

  @Test
  fun whenHttpErrorAssertExecuteFails() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString(statusCode = 404)

    val exception = apolloClient.query(HeroNameQuery()).execute().exception
    assertTrue(exception is ApolloHttpException)
    assertEquals(404, exception.statusCode)
  }

  @Test
  fun whenNetworkErrorAssertApolloNetworkException() = runTest(before = { setUp() }) {
    mockServer.close()

    assertTrue(apolloClient.query(HeroNameQuery()).execute().exception is ApolloNetworkException)
  }

  @Test
  fun WhenQueryAndMalformedNetworkResponseAssertSuccessAfterRetry() = runTest(before = { setUp() }, after = { tearDown() }) {
    mockServer.enqueueString("")
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    val response = apolloClient
        .query(query)
        .toFlow()
        .onEach {
          if (it.exception != null) {
            throw it.exception!!
          }
        }
        .retryWhen { _, attempt -> attempt == 0L }
        .single()

    assertEquals(data, response.data)
  }
}
