package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.cacheInfo
import com.apollographql.apollo3.cache.normalized.fetchFromCache
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.singleOrNull

/**
 *
 */
internal class CacheOnlyInterceptor : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(
        request = request
            .newBuilder()
            .fetchFromCache(true)
            .build()
    )
  }
}

internal class NetworkOnlyInterceptor : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request)
  }
}

/**
 * An interceptor that goes to cache first and then to the network if it fails
 */
internal class CacheFirstInterceptor : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      var cacheException: ApolloException? = null
      var networkException: ApolloException? = null

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
                        .cacheException(cacheException as? CacheMissException)
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

internal class NetworkFirstInterceptor : ApolloInterceptor {
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
