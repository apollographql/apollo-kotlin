package com.apollographql.apollo3.interceptor.cache

import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.internal.normalize
import com.apollographql.apollo3.cache.normalized.internal.readDataFromCache
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.interceptor.ApolloRequestInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class ApolloCacheInterceptor : ApolloRequestInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      when (request.executionContext[FetchPolicy] ?: FetchPolicy.CacheFirst) {
        FetchPolicy.CacheFirst -> {
          val response = readFromCache(request, chain.responseAdapterCache)
          if (response.data != null) {
            emit(response)
          } else {
            proceed(request, chain).collect { emit(it) }
          }
        }
        FetchPolicy.NetworkFirst -> {
          proceed(request, chain)
              .catch {
                val response = readFromCache(request, chain.responseAdapterCache)
                if (response.data != null) {
                  emit(response)
                } else {
                  // If we didn't get something in the cache, we need to signal callers that something went
                  // wrong and rethrow the network error
                  throw it
                }
              }
              .collect {
                emit(it)
              }
        }
        FetchPolicy.CacheOnly -> {
          val response = readFromCache(request, chain.responseAdapterCache)
          if (response.data != null) {
            emit(response)
          }
        }
        FetchPolicy.NetworkOnly -> {
          proceed(request, chain)
              .collect {
                emit(it)
              }
        }
        FetchPolicy.CacheAndNetwork -> {
          val response = readFromCache(request, chain.responseAdapterCache)
          if (response.data != null) {
            emit(response)
          }

          proceed(request, chain).collect {
            emit(it)
          }
        }
      }
    }
  }

  private fun <D : Operation.Data> proceed(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request).map {
      if (it.data != null) {
        writeToCache(request, it.data!!, chain.responseAdapterCache)
      }
      it.setFromCache(false)
    }
  }


  private fun <D : Operation.Data> ApolloResponse<D>.setFromCache(fromCache: Boolean): ApolloResponse<D> {
    return copy(executionContext = executionContext + CacheInfo(fromCache))
  }

  private fun <D : Operation.Data> writeToCache(request: ApolloRequest<D>, data: D, responseAdapterCache: ResponseAdapterCache) {
    val store = request.executionContext[ApolloStore] ?: error("No ApolloStore found")

    val operation = request.operation
    val records = operation.normalize(data, responseAdapterCache, CacheKeyResolver.DEFAULT)

    store.merge(records.values.toList(), CacheHeaders.NONE)
  }

  private fun <D : Operation.Data> readFromCache(request: ApolloRequest<D>, responseAdapterCache: ResponseAdapterCache): ApolloResponse<D> {
    val store = request.executionContext[ApolloStore] ?: error("No ApolloStore found")
    val operation = request.operation

    val data = operation.readDataFromCache(responseAdapterCache, store, CacheKeyResolver.DEFAULT, CacheHeaders.NONE)

    return ApolloResponse(
        requestUuid = request.requestUuid,
        operation = operation,
        data = data,
        executionContext = request.executionContext + CacheInfo(true)
    )
  }
}
