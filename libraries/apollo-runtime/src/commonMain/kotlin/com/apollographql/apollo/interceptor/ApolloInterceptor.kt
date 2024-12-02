package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ApolloResponse
import kotlinx.coroutines.flow.Flow

/**
 * An [ApolloInterceptor] intercepts requests at the GraphQL layer.
 *
 * [ApolloInterceptor]s may modify the request and or the response:
 *
 * ```kotlin
 * // Add a header to each request and extensions to each response.
 * val newRequest = request.newBuilder()
 *     .addHttpHeader("foo1", "bar1")
 *     .build()
 *
 * return chain.proceed(newRequest).map {
 *   it.newBuilder()
 *       .extensions(mapOf("foo2" to "bar2"))
 *       .build()
 * }
 * ```
 *
 * @see [com.apollographql.apollo.api.ExecutionContext]
 * @see [com.apollographql.apollo.network.http.HttpInterceptor]
 */
interface ApolloInterceptor {
  /**
   * Intercepts the given [request]. Call `chain.proceed()` to continue with the next interceptor or emit items
   * directly to bypass the chain.
   *
   * Exceptions thrown from [intercept] are not caught and are propagated to the call site. Use `ApolloResponse.Builder.exception()`
   * to return an exception response if you don't want your [com.apollographql.apollo.ApolloCall] to throw.
   *
   * @see ApolloResponse.Builder
   */
  fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>>
}

/**
 * An [ApolloInterceptorChain] is a list of interceptors called in order. Each interceptor wraps the subsequent ones
 * and can delegate to them by calling [proceed] or emit items directly.
 */
interface ApolloInterceptorChain {
  fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flow<ApolloResponse<D>>
}


internal class DefaultInterceptorChain(
    private val interceptors: List<ApolloInterceptor>,
    private val index: Int,
) : ApolloInterceptorChain {

  override fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    check(index < interceptors.size)
    return interceptors[index].intercept(
        request,
        DefaultInterceptorChain(
            interceptors = interceptors,
            index = index + 1,
        )
    )
  }
}
