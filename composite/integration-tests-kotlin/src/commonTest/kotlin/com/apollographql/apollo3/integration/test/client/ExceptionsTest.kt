package com.apollographql.apollo3.integration.test.client

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.integration.mockserver.MockResponse
import com.apollographql.apollo3.integration.mockserver.MockServer
import com.apollographql.apollo3.integration.enqueue
import com.apollographql.apollo3.integration.normalizer.HeroNameQuery
import com.apollographql.apollo3.testing.runWithMainLoop
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
  fun `when query and malformed network response, assert Exception`() = runWithMainLoop {
    mockServer.enqueue("malformed")

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery())
    }

    assertTrue(result.exceptionOrNull() != null)
  }

  @Test
  fun `when http error, assert execute fails`() = runWithMainLoop {
    mockServer.enqueue(MockResponse(statusCode = 404))

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery())
    }

    val exception = result.exceptionOrNull()
    assertTrue(exception is ApolloHttpException)
    assertEquals(404, exception.statusCode)
  }

  @Test
  fun `when network error, assert ApolloNetworkException`() = runWithMainLoop {
    mockServer.stop()

    val result = kotlin.runCatching {
      apolloClient.query(HeroNameQuery())
    }

    val exception = result.exceptionOrNull()
    assertTrue(exception is ApolloNetworkException)
  }

  @Test
  fun `when query and malformed network response, assert success after retry`() {
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