package com.apollographql.apollo.cache.http.internal

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.cache.http.CachingHttpInterceptor
import com.apollographql.apollo.cache.http.HttpFetchPolicy
import com.apollographql.apollo.cache.http.HttpFetchPolicyContext
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import java.io.IOException

internal class HttpCacheApolloInterceptor(
    private val apolloRequestToCacheKey: MutableMap<String, String>,
    private val cachingHttpInterceptor: CachingHttpInterceptor,
) : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val policy = getPolicy(request)
    val policyStr = when (policy) {
      HttpFetchPolicy.CacheFirst -> CachingHttpInterceptor.CACHE_FIRST
      HttpFetchPolicy.CacheOnly -> CachingHttpInterceptor.CACHE_ONLY
      HttpFetchPolicy.NetworkFirst -> CachingHttpInterceptor.NETWORK_FIRST
      HttpFetchPolicy.NetworkOnly -> CachingHttpInterceptor.NETWORK_ONLY
    }

    return chain.proceed(
        request.newBuilder()
            .addHttpHeader(
                CachingHttpInterceptor.CACHE_OPERATION_TYPE_HEADER,
                when (request.operation) {
                  is Query<*> -> "query"
                  is Mutation<*> -> "mutation"
                  is Subscription<*> -> "subscription"
                  else -> error("Unknown operation type")
                }
            )
            .addHttpHeader(CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, policyStr)
            .addHttpHeader(CachingHttpInterceptor.REQUEST_UUID_HEADER, request.requestUuid.toString())
            .addHttpHeader(CachingHttpInterceptor.OPERATION_NAME_HEADER, request.operation.name())
            .build()
    )
        .run {
          if (request.operation is Query<*>) {
            onEach { response ->
              // Revert caching of responses with errors
              val cacheKey = synchronized(apolloRequestToCacheKey) { apolloRequestToCacheKey[request.requestUuid.toString()] }
              if (response.hasErrors() || response.exception != null) {
                try {
                  cacheKey?.let { cachingHttpInterceptor.cache.remove(it) }
                } catch (_: IOException) {
                }
              }
            }.onCompletion {
              synchronized(apolloRequestToCacheKey) { apolloRequestToCacheKey.remove(request.requestUuid.toString()) }
            }
          } else {
            this
          }
        }
  }

  private fun getPolicy(request: ApolloRequest<*>): HttpFetchPolicy {
    return if (request.operation is Mutation<*>) {
      // Don't cache mutations
      HttpFetchPolicy.NetworkOnly
    } else {
      request.executionContext[HttpFetchPolicyContext]?.httpFetchPolicy ?: HttpFetchPolicy.CacheFirst
    }
  }
}
