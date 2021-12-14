package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.dependentKeys
import com.apollographql.apollo3.cache.normalized.isRefetching
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

internal class WatcherInterceptor(val store: ApolloStore) : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    if (!request.watch) {
      return chain.proceed(request)
    }

    check(request.operation is Query) {
      "It's impossible to watch a mutation or subscription"
    }

    val customScalarAdapters = request.executionContext[CustomScalarAdapters]!!
    var watchedKeys: Set<String>? = null

    return chain.proceed(request)
        .map {
          @Suppress("USELESS_CAST")
          it as ApolloResponse<D>?
        }
        .catch {
          if (it is ApolloException) {
            // Watchers ignore errors, but we still need to start watching the store
            emit(null)
          } else {
            throw it
          }
        }.onEach { response ->
          if (response?.data != null) {
            watchedKeys = store.normalize(request.operation, response.data!!, customScalarAdapters).values.dependentKeys()
          }
        }.onCompletion {
          store.changedKeys.filter { changedKeys ->
            watchedKeys == null || changedKeys.intersect(watchedKeys!!).isNotEmpty()
          }.map {
            chain.proceed(request.newBuilder().isRefetching(true).build())
                .catch {
                  if (it !is ApolloException) {
                    // Re-throw cancellation exceptions
                    throw it
                  }
                  // Else just ignore errors
                }
          }.flattenConcatPolyfill()
              .onEach { response ->
                if (response.data != null) {
                  watchedKeys = store.normalize(request.operation, response.data!!, customScalarAdapters).values.dependentKeys()
                }
              }.collect {
                emit(it)
              }
        }.filterNotNull()
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