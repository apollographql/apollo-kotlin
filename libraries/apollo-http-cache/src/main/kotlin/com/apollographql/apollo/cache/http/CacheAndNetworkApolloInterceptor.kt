package com.apollographql.apollo.cache.http

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.valueOf
import com.apollographql.apollo.cache.http.CachingHttpInterceptor.Companion.CACHE_FETCH_POLICY_HEADER
import com.apollographql.apollo.cache.http.CachingHttpInterceptor.Companion.CACHE_ONLY
import com.apollographql.apollo.cache.http.CachingHttpInterceptor.Companion.NETWORK_ONLY
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

class CacheAndNetworkApolloInterceptor : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val headers = request.httpHeaders.orEmpty()
    val fetchPolicy = headers.valueOf(CACHE_FETCH_POLICY_HEADER)

    return if (fetchPolicy == CachingHttpInterceptor.CACHE_AND_NETWORK) {
      flow {
        listOf(CACHE_ONLY, NETWORK_ONLY).forEach { policy ->
          val newRequest = request.newBuilder()
              .httpHeaders(headers.filterNot { it.name == CACHE_FETCH_POLICY_HEADER })
              .addHttpHeader(CACHE_FETCH_POLICY_HEADER, policy)
              .build()

          emit(chain.proceed(newRequest).single())
        }
      }
    } else {
      chain.proceed(request)
    }
  }
}