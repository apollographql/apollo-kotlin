package test

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.mockserver.MockResponse
import com.apollographql.apollo3.mockserver.MockServer
import com.apollographql.apollo3.mockserver.enqueue
import com.apollographql.apollo3.testing.runWithMainLoop
import com.apollographql.apollo3.testing.enqueue
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExceptionsTest {
  private lateinit var mockServer: MockServer
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    mockServer = MockServer()
    apolloClient = ApolloClient(mockServer.url())
  }

  @Test
  fun whenQueryAndMalformedNetworkResponseAssertException() = runWithMainLoop {
    mockServer.enqueue("malformed")

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery())
    }

    assertTrue(result.exceptionOrNull() != null)
  }

  @Test
  fun whenHttpErrorAssertExecuteFails() = runWithMainLoop {
    mockServer.enqueue(MockResponse(statusCode = 404))

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery())
    }

    val exception = result.exceptionOrNull()
    assertTrue(exception is ApolloHttpException)
    assertEquals(404, exception.statusCode)
  }

  @Test
  fun whenNetworkErrorAssertApolloNetworkException() = runWithMainLoop {
    mockServer.stop()

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery())
    }

    val exception = result.exceptionOrNull()
    assertTrue(exception is ApolloNetworkException)
  }

  @Test
  fun WhenQueryAndMalformedNetworkResponseAssertSuccessAfterRetry() {
    mockServer.enqueue("")
    val query = HeroNameQuery()
    val data = HeroNameQuery.Data(HeroNameQuery.Data.Hero("R2-D2"))
    mockServer.enqueue(query, data)

    val response = runWithMainLoop {
      apolloClient
          .queryAsFlow(ApolloRequest(query))
          .retryWhen { _, attempt -> attempt == 0L }
          .single()
    }

    assertEquals(data, response.data)
  }
}