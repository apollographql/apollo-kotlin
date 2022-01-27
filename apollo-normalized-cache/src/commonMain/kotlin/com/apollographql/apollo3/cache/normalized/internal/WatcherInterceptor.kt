package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.WatchErrorHandling
import com.apollographql.apollo3.cache.normalized.api.dependentKeys
import com.apollographql.apollo3.cache.normalized.isRefetching
import com.apollographql.apollo3.cache.normalized.watchContext
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

internal class WatcherInterceptor(val store: ApolloStore) : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val watchContext = request.watchContext ?: return chain.proceed(request)

    check(request.operation is Query) {
      "It's impossible to watch a mutation or subscription"
    }

    val customScalarAdapters = request.executionContext[CustomScalarAdapters]!!
    var watchedKeys: Set<String>? = null
    var isRefetching = false

    return store.changedKeys
        .onStart {
          // Trigger the initial fetch
          emit(emptySet())
        }
        .filter { changedKeys ->
          watchedKeys == null || changedKeys.intersect(watchedKeys!!).isNotEmpty()
        }.map {
          chain.proceed(request.newBuilder().isRefetching(isRefetching).build())
              .catch {
                if (it !is ApolloException) {
                  // Re-throw cancellation exceptions
                  throw it
                }
                // Else throw it or not according to error handling policy
                maybeThrow(it, if (isRefetching) watchContext.refetchErrorHandling else watchContext.fetchErrorHandling)
              }
              .onEach { response ->
                if (response.data != null) {
                  watchedKeys = store.normalize(request.operation, response.data!!, customScalarAdapters).values.dependentKeys()
                }
              }
              .onCompletion {
                isRefetching = true
              }
        }.flattenConcatPolyfill()
  }

  private fun maybeThrow(exception: ApolloException, errorHandling: WatchErrorHandling) {
    if (errorHandling == WatchErrorHandling.IGNORE_ERRORS) return
    val throwCacheErrors = errorHandling == WatchErrorHandling.THROW_CACHE_AND_NETWORK_ERRORS || errorHandling == WatchErrorHandling.THROW_CACHE_ERRORS
    val throwNetworkErrors = errorHandling == WatchErrorHandling.THROW_CACHE_AND_NETWORK_ERRORS || errorHandling == WatchErrorHandling.THROW_NETWORK_ERRORS
    when (exception) {
      is ApolloCompositeException -> {
        if (errorHandling == WatchErrorHandling.THROW_CACHE_AND_NETWORK_ERRORS) {
          throw exception
        }
        val cacheMissException = exception.first as? CacheMissException ?: exception.second as? CacheMissException
        // If it's *not* a CacheMissException we consider it a network error (could be ApolloNetworkException, ApolloHttpException, ApolloParseException...)
        val networkException = exception.first.takeIf { it !is CacheMissException } ?: exception.second.takeIf { it !is CacheMissException }
        if (cacheMissException != null && throwCacheErrors) {
          throw cacheMissException
        }
        if (networkException != null && throwNetworkErrors) {
          throw networkException
        }
      }
      is CacheMissException -> if (throwCacheErrors) {
        throw exception
      }
      // Treat all other exceptions as network errors (could be ApolloNetworkException, ApolloHttpException, ApolloParseException...)
      else -> if (throwNetworkErrors) {
        throw exception
      }
    }
  }
}

/**
 * A copy/paste of the kotlinx.coroutines version until it becomes stable
 *
 * This is taken from 1.5.2 and replacing `unsafeFlow {}` with `flow {}`
 */
private fun <T> Flow<Flow<T>>.flattenConcatPolyfill(): Flow<T> = flow {
  collect { value -> emitAll(value) }
}
