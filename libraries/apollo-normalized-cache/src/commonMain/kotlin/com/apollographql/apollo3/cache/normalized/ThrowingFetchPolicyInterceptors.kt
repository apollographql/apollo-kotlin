package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
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

/**
 * An interceptor that goes to cache first and then to the network if it fails.
 * Network errors are thrown as an [ApolloCompositeException] with the cache error as the first cause.
 *
 * This is the same behavior as CacheFirstInterceptor in 3.x and is kept here as a convenience for migration.
 */
val ThrowingCacheFirstInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val cacheException: ApolloException?
      var networkException: ApolloException? = null

      val cacheResponse = chain.proceed(
          request = request
              .newBuilder()
              .fetchFromCache(true)
              .build()
      ).single()
      cacheException = cacheResponse.exception

      if (cacheException == null) {
        emit(cacheResponse)
        return@flow
      }

      val networkResponses = chain.proceed(
          request = request
      ).onEach { response ->
        if (networkException == null) networkException = response.exception
      }.filter { response ->
        response.exception == null
      }.map { response ->
        response.newBuilder()
            .cacheInfo(
                response.cacheInfo!!
                    .newBuilder()
                    .cacheMissException(cacheException as? CacheMissException)
                    .build()
            )
            .build()
      }

      emitAll(networkResponses)

      if (networkException != null) {
        throw ApolloCompositeException(
            first = cacheException,
            second = networkException
        )
      }
    }
  }
}

/**
 * An interceptor that goes to the network first and then to cache if it fails.
 * Cache errors are thrown as an [ApolloCompositeException] with the network error as the first cause.
 *
 * This is the same behavior as NetworkFirstInterceptor in 3.x and is kept here as a convenience for migration.
 */
val ThrowingNetworkFirstInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val cacheException: ApolloException?
      var networkException: ApolloException? = null

      val networkResponses = chain.proceed(
          request = request
      ).onEach { response ->
        if (networkException == null) networkException = response.exception
      }.filter { response ->
        response.exception == null
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
      cacheException = cacheResponse.exception

      if (cacheException == null) {
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
          first = networkException,
          second = cacheException,
      )
    }
  }
}

/**
 * An interceptor that goes to cache first and then to the network.
 * An exception is not thrown if the cache fails, whereas an exception will be thrown upon network failure.
 *
 * This is the same behavior as CacheAndNetworkInterceptor in 3.x and is kept here as a convenience for migration.
 */
val ThrowingCacheAndNetworkInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val cacheException: ApolloException?
      var networkException: ApolloException? = null

      val cacheResponse = chain.proceed(
          request = request
              .newBuilder()
              .fetchFromCache(true)
              .build()
      ).single()
      cacheException = cacheResponse.exception

      if (cacheException == null) {
        emit(cacheResponse.newBuilder().isLast(false).build())
      }

      val networkResponses = chain.proceed(request)
          .onEach { response ->
            if (networkException == null) networkException = response.exception
          }
          .map { response ->
            response.newBuilder()
                .cacheInfo(
                    response.cacheInfo!!
                        .newBuilder()
                        .cacheMissException(cacheException as? CacheMissException)
                        .build()
                )
                .build()
          }
          .filter { response ->
            response.exception == null
          }

      emitAll(networkResponses)

      if (networkException != null) {
        if (cacheException != null) {
          throw ApolloCompositeException(
              first = cacheException,
              second = networkException
          )
        }
        throw networkException!!
      }
    }
  }
}
