package com.apollographql.apollo3.interceptor.cache

import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.internal.ReadableStore
import com.apollographql.apollo3.cache.normalized.internal.WriteableStore
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.Response
import com.apollographql.apollo3.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo3.interceptor.ApolloResponse
import com.apollographql.apollo3.cache.normalized.internal.normalize
import com.apollographql.apollo3.cache.normalized.internal.readDataFromCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ApolloExperimental
class ApolloCacheInterceptor : ApolloRequestInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      when (request.executionContext[FetchPolicy] ?: FetchPolicy.CACHE_FIRST) {
        FetchPolicy.CACHE_FIRST -> {
          val response = readFromCache(request, chain.responseAdapterCache)
          if (response?.response?.data != null) {
            emit(response)
          } else {
            proceed(request, chain).collect { emit(it) }
          }
        }
        FetchPolicy.NETWORK_FIRST -> {
          proceed(request, chain)
              .catch {
                val response = readFromCache(request, chain.responseAdapterCache)
                if (response?.response?.data != null) {
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
        FetchPolicy.CACHE_ONLY -> {
          val response = readFromCache(request, chain.responseAdapterCache)
          if (response?.response?.data != null) {
            emit(response)
          }
        }
        FetchPolicy.NETWORK_ONLY -> {
          proceed(request, chain)
              .collect {
                emit(it)
              }
        }
        FetchPolicy.CACHE_AND_NETWORK -> {
          val response = readFromCache(request, chain.responseAdapterCache)
          if (response?.response?.data != null) {
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
      if (it.response.data != null) {
        writeToCache(request, it.response.data!!, chain.responseAdapterCache)
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

  private fun <D : Operation.Data> readFromCache(request: ApolloRequest<D>, responseAdapterCache: ResponseAdapterCache): ApolloResponse<D>? {
    val store = request.executionContext[ApolloStore] ?: error("No ApolloStore found")
    val operation = request.operation

    val data = operation.readDataFromCache(responseAdapterCache, store, CacheKeyResolver.DEFAULT, CacheHeaders.NONE)

    return ApolloResponse(
        response = Response(
            operation = operation,
            data = data
        ),
        requestUuid = request.requestUuid,
        executionContext = request.executionContext
    ).setFromCache(true)
  }
}
