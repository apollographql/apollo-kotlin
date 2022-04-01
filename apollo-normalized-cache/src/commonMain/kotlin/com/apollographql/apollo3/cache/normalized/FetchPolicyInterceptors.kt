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
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlin.jvm.JvmName

/**
 * An interceptor that goes to the cache only
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

      emitAll(networkResponse)

      if (networkException != null) {
        throw ApolloCompositeException(
            first = cacheException,
            second = networkException
        )
      }
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
      }

      emitAll(networkResponse)
      if (networkException == null) {
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
          first = networkException,
          second = cacheException,
      )
    }
  }
}

/**
 * An interceptor that goes to cache first and then to the network.
 * An exception is not thrown if the cache fails, whereas an exception will be thrown upon network failure.
 */
val CacheAndNetworkInterceptor = object : ApolloInterceptor {
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
        emit(cacheResponse.newBuilder().isLast(false).build())
      }

      val networkResponse = chain.proceed(request)
          .catch {
            if (it is ApolloException) {
              networkException = it
            } else {
              throw it
            }
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

      emitAll(networkResponse)

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

internal val FetchPolicyRouterInterceptor = object : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    if (request.operation !is Query) {
      // Subscriptions and Mutations do not support fetchPolicies
      return chain.proceed(request)
    }
    return request.fetchPolicyInterceptor.intercept(request, chain)
  }
}
