package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.parseResponseBody
import com.apollographql.apollo3.api.exception.ApolloHttpException
import com.apollographql.apollo3.interceptor.BearerTokenInterceptor
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.HttpRequestParameters
import com.apollographql.apollo3.testing.MockQuery
import com.apollographql.apollo3.testing.TestTokenProvider
import com.apollographql.apollo3.testing.runBlocking
import com.apollographql.apollo3.testing.runWithMainLoop
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okio.ByteString.Companion.encodeUtf8
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OauthInterceptorTest {
  class AuthenticatedNetworkTransport : NetworkTransport {
    companion object {
      const val VALID_ACCESS_TOKEN1 = "VALID_ACCESS_TOKEN1"
      const val VALID_ACCESS_TOKEN2 = "VALID_ACCESS_TOKEN2"
      const val INVALID_ACCESS_TOKEN = "INVALID_ACCESS_TOKEN"
    }

    override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
      val authorization = request.executionContext[HttpRequestParameters]?.headers?.get("Authorization")

      return flow {
        when (authorization) {
          "Bearer $VALID_ACCESS_TOKEN1",
          "Bearer $VALID_ACCESS_TOKEN2" -> {
            emit(request.operation.parseResponseBody("{\"data\":{\"name\":\"MockQuery\"}}".encodeUtf8()).copy(
                requestUuid = request.requestUuid,
                executionContext = ExecutionContext.Empty
            ))
          }
          else -> {
            throw ApolloHttpException(
                message = "Http request failed with status code `401`",
                statusCode = 401,
                headers = emptyMap()
            )
          }
        }
      }
    }

    override fun dispose() {
      TODO("Not yet implemented")
    }
  }

  private fun apolloClient(currentAccessToken: String, newAccessToken: String): ApolloClient {
    val networkTransport = AuthenticatedNetworkTransport()
    return ApolloClient(networkTransport).withInterceptor(
            BearerTokenInterceptor(
                TestTokenProvider(
                    currentAccessToken,
                    newAccessToken
                )
            )
        )
  }

  @Test
  fun `valid access token succeeds`() {
    val response = runWithMainLoop {
      apolloClient(AuthenticatedNetworkTransport.VALID_ACCESS_TOKEN1,
          AuthenticatedNetworkTransport.VALID_ACCESS_TOKEN2)
          .query(ApolloRequest(MockQuery()))
    }

    assertNotNull(response.data)
    assertEquals(expected = MockQuery.Data, actual = response.data)
  }

  @Test
  fun `invalid access token fails`() {
    val result = runWithMainLoop {
      kotlin.runCatching {
        apolloClient(AuthenticatedNetworkTransport.INVALID_ACCESS_TOKEN,
            AuthenticatedNetworkTransport.INVALID_ACCESS_TOKEN)
            .query(ApolloRequest(MockQuery()))
      }
    }

    assertTrue(result.isFailure)
  }

  @Test
  fun `refresh access token succeeds`() {
    val response = runWithMainLoop {
      apolloClient(AuthenticatedNetworkTransport.INVALID_ACCESS_TOKEN,
          AuthenticatedNetworkTransport.VALID_ACCESS_TOKEN2)
          .query(ApolloRequest(MockQuery()))
    }

    assertNotNull(response.data)
    assertEquals(expected = MockQuery.Data, actual = response.data)
  }
}
