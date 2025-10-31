package test

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.DefaultApolloException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.testing.internal.runTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okio.use
import kotlin.test.Test
import kotlin.test.assertNotNull

class InterceptorTest {
  @Test
  fun interceptorsCanBeAddedAfterCache() = runTest {
    ApolloClient.Builder()
        .networkTransport(object : NetworkTransport {
          override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
            return flowOf(ApolloResponse.Builder(request.operation, request.requestUuid).exception(DefaultApolloException("unused")).build())
          }

          override fun dispose() {}
        })
        .cacheInterceptor(object : ApolloInterceptor {
          override fun <D : Operation.Data> intercept(
              request: ApolloRequest<D>,
              chain: ApolloInterceptorChain,
          ): Flow<ApolloResponse<D>> {
            return chain.proceed(request.newBuilder().addExecutionContext(CacheExecutionContext()).build())
          }
        })
        .addInterceptor(object : ApolloInterceptor {
          override fun <D : Operation.Data> intercept(
              request: ApolloRequest<D>,
              chain: ApolloInterceptorChain,
          ): Flow<ApolloResponse<D>> {
            // check that we are called after the cache
            assertNotNull(request.executionContext[CacheExecutionContext])
            return chain.proceed(request)
          }
        }, ApolloInterceptor.InsertionPoint.BeforeNetwork)
        .build()
        .use {
          it.query(FooQuery()).execute()
        }
  }
}

class CacheExecutionContext: ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key: ExecutionContext.Key<CacheExecutionContext>
}