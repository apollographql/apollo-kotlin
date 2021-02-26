package com.apollographql.apollo3.integration

import HeroNameQuery
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloInternal
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.testing.TestHttpEngine
import com.apollographql.apollo3.testing.TestLoggerExecutor
import com.apollographql.apollo3.testing.runBlocking
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
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
    testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))

    val response = runBlocking {
      apolloClient
          .query(HeroNameQuery())
          .execute()
          .single()
    }

    assertEquals(response.data?.hero?.name, "R2-D2")
  }

  @Test
  fun `when query and malformed network response, assert parse error`() {
    testHttpEngine.offer("malformed")

    val result = runBlocking {
      kotlin.runCatching {
        apolloClient
            .query(HeroNameQuery())
            .execute()
            .single()
      }
    }

    assertTrue(result.isFailure)
  }

  @Test
  fun `when query and malformed network response, assert success after retry`() {
    testHttpEngine.offer("")
    testHttpEngine.offer(fixtureResponse("HeroNameResponse.json"))

    val response = runBlocking {
      apolloClient
          .query(HeroNameQuery())
          .execute()
          .retryWhen { _, attempt -> attempt == 0L }
          .single()
    }

    assertEquals(response.data?.hero?.name, "R2-D2")
  }
}