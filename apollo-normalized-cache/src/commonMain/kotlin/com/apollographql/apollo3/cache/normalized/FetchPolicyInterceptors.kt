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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.singleOrNull
import kotlin.jvm.JvmName

/**
 *
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

val NetworkOnlyInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request)
  }
}

/**
 * An interceptor that goes to cache first and then to the network if it fails
 */
val CacheFirstInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      var cacheException: ApolloException? = null
      var networkException: ApolloException? = null

      val cacheResponse = chain.proceed(
          request = request
              .newBuilder()
              .fetchFromCache(true)
              .build()
      ).catch { throwable ->
        if (throwable is ApolloException) {
          cacheException = throwable
        } else {
          throw throwable
        }
      }.singleOrNull()

      if (cacheResponse != null) {
        emit(cacheResponse)
        return@flow
      }

      val networkResponse = chain.proceed(
          request = request
      ).catch {
        if (it is ApolloException) {
          networkException = it
        } else {
          throw it
        }
      }.singleOrNull()

      if (networkResponse != null) {
        emit(
            networkResponse.newBuilder()
                .cacheInfo(
                    networkResponse.cacheInfo!!
                        .newBuilder()
                        .cacheMissException(cacheException as? CacheMissException)
                        .build()
                )
                .build()
        )
        return@flow
      }

      throw ApolloCompositeException(
          cacheException,
          networkException
      )
    }
  }
}

val NetworkFirstInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      var cacheException: ApolloException? = null
      var networkException: ApolloException? = null

      val networkResponse = chain.proceed(
          request = request
      ).catch {
        if (it is ApolloException) {
          networkException = it
        } else {
          throw it
        }
      }.singleOrNull()

      if (networkResponse != null) {
        emit(networkResponse)
        return@flow
      }

      val cacheResponse = chain.proceed(
          request = request
              .newBuilder()
              .fetchFromCache(true)
              .build()
      ).catch {
        if (it is ApolloException) {
          cacheException = it
        } else {
          throw it
        }
      }.singleOrNull()

      if (cacheResponse != null) {
        emit(
            cacheResponse.newBuilder()
                .cacheInfo(
                    cacheResponse.cacheInfo!!
                        .newBuilder()
                        .networkException(networkException)
                        .build()
                )
                .build()
        )
        return@flow
      }

      throw ApolloCompositeException(
          networkException,
          cacheException,
      )
    }
  }
}

internal val FetchPolicyRouterInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    if (request.operation !is Query) {
      // Subscriptions and Mutations do not support fetchPolicies
      return chain.proceed(request)
    }
    return if (!request.isRefetching) {
      request.fetchPolicyInterceptor.intercept(request, chain)
    } else {
      request.refetchPolicyInterceptor.intercept(request, chain)
    }
  }
}