package test.network

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.http.HttpInfo
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueError
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import okio.use
import test.FooQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HttpNetworkTransportTest {
  @Test
  fun graphqlResponseJsonContentTypeMayHaveNon2xxHttpCode() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .build()
          .use { apolloClient ->
            mockServer.enqueueString(string = FooQuery.errorResponse, statusCode = 500, contentType = "application/graphql-response+json")
            val response = apolloClient.query(FooQuery()).execute()
            assertEquals("Oh no! Something went wrong :(", response.errors?.single()?.message)
          }
    }
  }

  @Test
  fun postContentTypeIsApplicationJson() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .build()
          .use { apolloClient ->
            mockServer.enqueueError(500)
            apolloClient.query(FooQuery()).execute()
            val headers = mockServer.takeRequest().headers
            assertEquals("application/json", headers.get("Content-Type"))
          }
    }
  }

  @Test
  fun interceptorCanRestoreThrowingBehaviour() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .addInterceptor(object : ApolloInterceptor {
            override fun <D : Operation.Data> intercept(
                request: ApolloRequest<D>,
                chain: ApolloInterceptorChain,
            ): Flow<ApolloResponse<D>> {
              return chain.proceed(request).onEach {
                val httpInfo = it.executionContext[HttpInfo]
                if (httpInfo != null && httpInfo.statusCode !in 200..299) {
                  throw ApolloHttpException(httpInfo.statusCode, httpInfo.headers, null, "HTTP request failed")
                }
              }
            }
          })
          .build()
          .use { apolloClient ->
            mockServer.enqueueString(string = FooQuery.errorResponse, statusCode = 500, contentType = "application/graphql-response+json")
            val exception = assertFailsWith<ApolloHttpException> {
              apolloClient.query(FooQuery()).execute()
            }

            assertEquals("HTTP request failed", exception.message)
          }
    }
  }
}