package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import kotlinx.coroutines.flow.Flow

interface ApolloInterceptorChain {
  fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flow<ApolloResponse<D>>
}

/**
 * Implementations can optionally implement [okio.Closeable], if they do, their [okio.Closeable.close] function will be called when the
 * [com.apollographql.apollo.ApolloClient] they are associated to is closed.
 */
interface ApolloInterceptor {
  fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>>
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
