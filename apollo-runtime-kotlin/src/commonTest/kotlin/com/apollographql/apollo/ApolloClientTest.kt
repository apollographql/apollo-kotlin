package com.apollographql.apollo

import com.apollographql.apollo.testing.MockNetworkTransport
import com.apollographql.apollo.testing.MockQuery
import com.apollographql.apollo.testing.TestLoggerExecutor
import com.apollographql.apollo.testing.runBlocking
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@Suppress("EXPERIMENTAL_API_USAGE")
class ApolloClientTest {
  private lateinit var networkTransport: MockNetworkTransport
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    networkTransport = MockNetworkTransport()
    apolloClient = ApolloClient.Builder()
        .networkTransport(networkTransport)
        .interceptors(TestLoggerExecutor)
        .build()
  }

  @Test
  fun `when query and success network response, assert success`() {
    networkTransport.offer("{\"data\":{\"name\":\"MockQuery\"}")

    val response = runBlocking {
      apolloClient
          .query(MockQuery())
          .execute()
          .single()
    }

    assertNotNull(response.data)
    assertEquals(expected = MockQuery.Data, actual = response.data)
  }

  @Test
  fun `when query and malformed network response, assert parse error`() {
    networkTransport.offer("")

    val result = runBlocking {
      kotlin.runCatching {
        apolloClient
            .query(MockQuery())
            .execute()
            .single()
      }
    }

    assertTrue(result.isFailure)
    result.onFailure { e ->
      assertTrue(e is ApolloParseException)
    }
  }

  @Test
  fun `when query and malformed network response, assert success after retry`() {
    networkTransport.offer("")
    networkTransport.offer("{\"data\":{\"name\":\"MockQuery\"}}")

    val response = runBlocking {
      apolloClient
          .query(MockQuery())
          .execute()
          .retryWhen { cause, attempt -> cause is ApolloException && attempt == 0L }
          .single()
    }

    assertNotNull(response.data)
    assertEquals(expected = MockQuery.Data, actual = response.data)
  }
}
