@file:JvmName("FetchPolicyInterceptors")

package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlin.jvm.JvmName

/**
 * An interceptor that emits the response from the cache only.
 */
val CacheOnlyInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(
        request = request
            .newBuilder()
            .fetchFromCache(true)
            .build()
    )
  }
}

/**
 * An interceptor that emits the response(s) from the network only.
 */
val NetworkOnlyInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request)
  }
}

/**
 * An interceptor that emits the response from the cache first, and if there was a cache miss, emits the response(s) from the network.
 */
val CacheFirstInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val cacheResponse = chain.proceed(
          request = request
              .newBuilder()
              .fetchFromCache(true)
              .build()
      ).single()
      if (cacheResponse.exception == null) {
        emit(cacheResponse)
        return@flow
      } else if (!request.foldFetchExceptions) {
        emit(cacheResponse.newBuilder().isLast(false).build())
      }

      val networkResponses = chain.proceed(
          request = request
      ).map { response ->
        response.newBuilder()
            .cacheInfo(
                response.cacheInfo!!
                    .newBuilder()
                    .networkException(response.exception)
                    .cacheMissException(cacheResponse.exception as? CacheMissException)
                    .build()
            )
            .apply {
              if (request.foldFetchExceptions && response.exception != null && cacheResponse.exception != null) {
                exception(ApolloCompositeException(cacheResponse.exception, response.exception))
              }
            }
            .build()
      }

      emitAll(networkResponses)
    }
  }
}

/**
 * An interceptor that emits the response(s) from the network first, and if there was a network error, emits the response from the cache.
 */
val NetworkFirstInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      var networkException: ApolloException? = null

      val networkResponses = chain.proceed(
          request = request
      ).onEach { response ->
        if (response.exception != null && networkException == null) {
          networkException = response.exception
        }
      }.map { response ->
        if (networkException != null) {
          response.newBuilder()
              .isLast(false)
              .build()
        } else {
          response
        }
      }.filter { !request.foldFetchExceptions || it.exception == null }

      emitAll(networkResponses)
      if (networkException == null) {
        return@flow
      }

      val cacheResponse = chain.proceed(
          request = request
              .newBuilder()
              .fetchFromCache(true)
              .build()
      ).single()
      emit(
          cacheResponse.newBuilder()
              .cacheInfo(
                  cacheResponse.cacheInfo!!
                      .newBuilder()
                      .networkException(networkException)
                      .cacheMissException(cacheResponse.exception as? CacheMissException)
                      .build()
              )
              .apply {
                if (request.foldFetchExceptions && cacheResponse.exception != null && networkException != null) {
                  exception(ApolloCompositeException(networkException, cacheResponse.exception))
                }
              }
              .build()
      )
    }
  }
}

/**
 * An interceptor that emits the response from the cache first, and then emits the response(s) from the network.
 */
val CacheAndNetworkInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val cacheResponse = chain.proceed(
          request = request
              .newBuilder()
              .fetchFromCache(true)
              .build()
      ).single()
      if (cacheResponse.exception == null || !request.foldFetchExceptions) {
        emit(cacheResponse.newBuilder().isLast(false).build())
      }

      val networkResponses = chain.proceed(request)
          .map { response ->
            response.newBuilder()
                .cacheInfo(
                    response.cacheInfo!!
                        .newBuilder()
                        .cacheMissException(cacheResponse.exception as? CacheMissException)
                        .networkException(response.exception)
                        .build()
                )
                .apply {
                  if (request.foldFetchExceptions && response.exception != null && cacheResponse.exception != null) {
                    exception(ApolloCompositeException(cacheResponse.exception, response.exception))
                  }
                }
                .build()
          }

      emitAll(networkResponses)
    }
  }
}

internal val FetchPolicyRouterInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    if (request.operation !is Query) {
      // Subscriptions and Mutations do not support fetchPolicies
      return chain.proceed(request)
    }
    return request.fetchPolicyInterceptor.intercept(request, chain)
  }
}
