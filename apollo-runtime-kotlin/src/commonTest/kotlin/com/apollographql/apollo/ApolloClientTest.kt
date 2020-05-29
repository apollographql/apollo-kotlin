package com.apollographql.apollo

import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.mock.MockNetworkTransport
import com.apollographql.apollo.mock.MockQuery
import com.apollographql.apollo.mock.TestLoggerExecutor
import com.apollographql.apollo.network.GraphQLResponse
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.single
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
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
    apolloClient = ApolloClient(
        networkTransport = networkTransport,
        interceptors = listOf(TestLoggerExecutor)
    )
  }

  @Test
  fun `when query and success network response, assert success`() {
    networkTransport.offer(
        GraphQLResponse(
            body = Buffer().write("{\"data\":{\"name\":\"MockQuery\"}}".encodeUtf8()),
            executionContext = ExecutionContext.Empty,
            requestUuid = uuid4()
        )
    )

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
    networkTransport.offer(
        GraphQLResponse(
            body = Buffer(),
            executionContext = ExecutionContext.Empty,
            requestUuid = uuid4()
        )
    )

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
    networkTransport.offer(
        GraphQLResponse(
            body = Buffer(),
            executionContext = ExecutionContext.Empty,
            requestUuid = uuid4()
        )
    )
    networkTransport.offer(
        GraphQLResponse(
            body = Buffer().write("{\"data\":{\"name\":\"MockQuery\"}}".encodeUtf8()),
            executionContext = ExecutionContext.Empty,
            requestUuid = uuid4()
        )
    )

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
