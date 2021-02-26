package com.apollographql.apollo3.integration

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloInternal
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.testing.MockQuery
import com.apollographql.apollo3.testing.TestHttpEngine
import com.apollographql.apollo3.testing.TestLoggerExecutor
import com.apollographql.apollo3.testing.runBlocking
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ApolloInternal::class)
class ApolloClientTest {
  private lateinit var testHttpEngine: TestHttpEngine
  private lateinit var apolloClient: ApolloClient

  @BeforeTest
  fun setUp() {
    testHttpEngine = TestHttpEngine()
    apolloClient = ApolloClient.Builder()
        .networkTransport(ApolloHttpNetworkTransport(serverUrl = "https://test", engine = testHttpEngine))
        .interceptors(TestLoggerExecutor)
        .build()
  }

  @Test
  fun `when query and success network response, assert success`() {
    testHttpEngine.offer("{\"data\":{\"name\":\"MockQuery\"}}")

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
    testHttpEngine.offer("malformed")

    val result = runBlocking {
      kotlin.runCatching {
        apolloClient
            .query(MockQuery())
            .execute()
            .single()
      }
    }

    assertTrue(result.isFailure)
  }

  @Test
  fun `when query and malformed network response, assert success after retry`() {
    testHttpEngine.offer("")
    testHttpEngine.offer("{\"data\":{\"name\":\"MockQuery\"}}")

    val response = runBlocking {
      apolloClient
          .query(MockQuery())
          .execute()
          .retryWhen { _, attempt -> attempt == 0L }
          .single()
    }

    assertNotNull(response.data)
    assertEquals(expected = MockQuery.Data, actual = response.data)
  }
}