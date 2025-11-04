import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.apollo.testing.internal.runTest
import com.apollographql.mockserver.MockServer
import com.apollographql.mockserver.enqueueString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okio.Buffer
import okio.use
import test.FooQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExecutionContextTest {
  @Test
  fun responseDoesNotContainRequestExecutionContext() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .addInterceptor(object : ApolloInterceptor {
            override fun <D : Operation.Data> intercept(
                request: ApolloRequest<D>,
                chain: ApolloInterceptorChain,
            ): Flow<ApolloResponse<D>> {
              assertNotNull(request.executionContext[MyExecutionContext])
              return chain.proceed(request)
            }

          })
          .build()
          .use { apolloClient ->

            mockServer.enqueueString(FooQuery.successResponse)
            val response = apolloClient.query(FooQuery())
                .addExecutionContext(MyExecutionContext())
                .execute()

            assertNull(response.executionContext[MyExecutionContext])
          }
    }
  }

  @Test
  fun responseDoesNotContainClientExecutionContext() = runTest {
    MockServer().use { mockServer ->
      ApolloClient.Builder()
          .serverUrl(mockServer.url())
          .addExecutionContext(MyExecutionContext())
          .build()
          .use { apolloClient ->

            mockServer.enqueueString(FooQuery.successResponse)
            val response = apolloClient.query(FooQuery())
                .execute()

            assertNull(response.executionContext[MyExecutionContext])
          }
    }
  }
}

internal class MyExecutionContext : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<MyExecutionContext>
}