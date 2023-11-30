@file:JvmName("FetchPolicyInterceptors")

package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.exception.DefaultApolloException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
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
      emit(cacheResponse.newBuilder().isLast(cacheResponse.exception == null).build())
      if (cacheResponse.exception == null) {
        return@flow
      }

      val networkResponses = chain.proceed(request = request)
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
      }

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
      emit(cacheResponse)
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

      emit(cacheResponse.newBuilder().isLast(false).build())

      val networkResponses = chain.proceed(request)
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

    if (request.useV3ExceptionHandling != true) {
      // Fast path
      return request.fetchPolicyInterceptor.intercept(request, chain)
    }
    return flow {
      val exceptions = mutableListOf<ApolloException>()
      var hasEmitted = false

      request.fetchPolicyInterceptor.intercept(request, chain)
          .collect {
            if (!hasEmitted && it.exception != null) {
              // Remember to send the exception later
              exceptions.add(it.exception!!)
              return@collect
            }
            emit(
                it.newBuilder()
                    .cacheInfo(
                        it.cacheInfo!!.newBuilder()
                            .cacheMissException(exceptions.filterIsInstance<CacheMissException>().firstOrNull())
                            .networkException(exceptions.firstOrNull { it !is CacheMissException })
                            .build()
                    )
                    .build()
            )
            hasEmitted = true
          }

      if (!hasEmitted) {
        // If we haven't emitted anything, send a composite exception
        val first = exceptions.firstOrNull()
        val exception = if (first == null) {
          DefaultApolloException("No response emitted")
        } else {
          first.also { firstException ->
            exceptions.drop(1).forEach {
              firstException.addSuppressed(it)
            }
          }
        }
        emit(
            ApolloResponse.Builder(request.operation, request.requestUuid, exception)
                .build()

        )
      }
    }
  }
}
