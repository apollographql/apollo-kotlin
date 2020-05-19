package com.apollographql.apollo

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.interceptor.AccessTokenProvider
import com.apollographql.apollo.interceptor.OauthInterceptor
import com.apollographql.apollo.mock.MockNetworkTransport
import com.apollographql.apollo.mock.MockQuery
import com.apollographql.apollo.mock.TestAccessTokenProvider
import com.apollographql.apollo.mock.TestLoggerExecutor
import com.apollographql.apollo.network.GraphQLRequest
import com.apollographql.apollo.network.GraphQLResponse
import com.apollographql.apollo.network.HttpExecutionContext
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.single
import okio.Buffer
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@ApolloExperimental
class OauthInterceptorTest {
  class AuthenticatedNetworkTransport : NetworkTransport {
    companion object {
      const val VALID_ACCESS_TOKEN1 = "VALID_ACCESS_TOKEN1"
      const val VALID_ACCESS_TOKEN2 = "VALID_ACCESS_TOKEN2"
      const val INVALID_ACCESS_TOKEN = "INVALID_ACCESS_TOKEN"
    }

    override fun execute(request: GraphQLRequest, executionContext: ExecutionContext): Flow<GraphQLResponse> {
      val authorization = executionContext[HttpExecutionContext.Request]?.headers?.get("Authorization")

      return flowOf(when (authorization) {
        "Bearer $VALID_ACCESS_TOKEN1",
        "Bearer $VALID_ACCESS_TOKEN2" -> {
          GraphQLResponse(
              body = Buffer().write("{\"data\":{\"name\":\"MockQuery\"}}".encodeUtf8()),
              executionContext = ExecutionContext.Empty
          )
        }
        else -> {
          throw ApolloException(
              message = "Http request failed with status code `401`",
              error = ApolloError.Network,
              executionContext = HttpExecutionContext.Response(
                  statusCode = 401,
                  headers = emptyMap()
              )
          )
        }
      })
    }
  }


  private fun apolloClient(currentAccessToken: String, newAccessToken: String): ApolloClient {
    val networkTransport = AuthenticatedNetworkTransport()
    return ApolloClient(
        networkTransport = networkTransport,
        interceptors = listOf(OauthInterceptor(TestAccessTokenProvider(
            currentAccessToken,
            newAccessToken
        )))
    )
  }

  @Test
  fun `valid access token succeeds`() {
    val response = runBlocking {
      apolloClient(AuthenticatedNetworkTransport.VALID_ACCESS_TOKEN1,
          AuthenticatedNetworkTransport.VALID_ACCESS_TOKEN2)
          .query(MockQuery()).execute().single()
    }

    assertNotNull(response.data)
    assertEquals(expected = MockQuery.Data, actual = response.data)
  }

  @Test
  fun `invalid access token fails`() {
    val result = runBlocking {
      kotlin.runCatching {
        apolloClient(AuthenticatedNetworkTransport.INVALID_ACCESS_TOKEN,
            AuthenticatedNetworkTransport.INVALID_ACCESS_TOKEN)
            .query(MockQuery()).execute().single()
      }
    }

    assertTrue(result.isFailure)
    result.onFailure { e ->
      assertTrue(e is ApolloException)
      assertTrue(e.error is ApolloError.Network)
    }
  }

  @Test
  fun `refresh access token succeeds`() {
    val response = runBlocking {
      apolloClient(AuthenticatedNetworkTransport.INVALID_ACCESS_TOKEN,
          AuthenticatedNetworkTransport.VALID_ACCESS_TOKEN2)
          .query(MockQuery()).execute().single()
    }

    assertNotNull(response.data)
    assertEquals(expected = MockQuery.Data, actual = response.data)
  }
}