import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okio.use
import test.FooQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class InterceptorsErrorTest {
  @Test
  fun interceptorsMayThrow() {
    ApolloClient.Builder()
        .serverUrl("unused")
        .addInterceptor(object : ApolloInterceptor {
          override fun <D : Operation.Data> intercept(
              request: ApolloRequest<D>,
              chain: ApolloInterceptorChain,
          ): Flow<ApolloResponse<D>> {
            throw IllegalStateException("woops")
          }
        })
        .build()
        .use { apolloClient ->

          runBlocking {
            val exception = assertFailsWith<IllegalStateException> {
              apolloClient.query(FooQuery())
                  .execute()
            }

            assertEquals("woops", exception.message)
          }
        }
  }

  @Test
  fun interceptorsMayReturnExceptionResponse() {
    ApolloClient.Builder()
        .serverUrl("unused")
        .addInterceptor(object : ApolloInterceptor {
          override fun <D : Operation.Data> intercept(
              request: ApolloRequest<D>,
              chain: ApolloInterceptorChain,
          ): Flow<ApolloResponse<D>> {
            return flow {
              emit(
                  ApolloResponse.Builder(request.operation, request.requestUuid)
                      .exception(DefaultApolloException("oh no", IllegalStateException("woops")))
                      .build()
              )
            }
          }
        })
        .build()
        .use { apolloClient ->
          runBlocking {
            apolloClient.query(FooQuery())
                .execute()
                .apply {
                  assertIs<DefaultApolloException>(exception)
                  assertIs<IllegalStateException>(exception!!.cause)
                  assertEquals("woops", exception!!.cause!!.message)
                }
          }
        }
  }
}