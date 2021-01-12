package com.apollographql.apollo.interceptor.cache

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response.Companion.builder
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.internal.CacheValueResolver
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.RealCacheKeyBuilder
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo.interceptor.ApolloResponse
import com.apollographql.apollo.api.parseData
import com.apollographql.apollo.cache.normalized.internal.normalize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ApolloExperimental
class ApolloCacheInterceptor<S>(private val store: S) : ApolloRequestInterceptor where S : WriteableStore, S : ReadableStore {

  private fun <D : Operation.Data> proceed(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return chain.proceed(request).map {
      if (it.response.data != null) {
        writeToCache(request, it.response.data!!, chain.customScalarAdapters)
      }
      it.setFromCache(false)
    }
  }

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val policy = request.executionContext[CacheRequestExecutionContext]?.policy ?: FetchPolicy.CACHE_FIRST

      when (policy) {
        FetchPolicy.CACHE_FIRST -> {
          val response = readFromCache(request, chain.customScalarAdapters)
          if (response != null) {
            emit(response)
          } else {
            proceed(request, chain).collect { emit(it) }
          }
        }
        FetchPolicy.NETWORK_FIRST -> {
          proceed(request, chain)
              .catch {
                val response = readFromCache(request, chain.customScalarAdapters)
                if (response != null) {
                  emit(response)
                }
              }
              .collect {
                emit(it)
              }
        }
        FetchPolicy.CACHE_ONLY -> {
          val response = readFromCache(request, chain.customScalarAdapters)
          if (response != null) {
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
          val response = readFromCache(request, chain.customScalarAdapters)
          if (response != null) {
            emit(response)
          }

          proceed(request, chain).collect {
            emit(it)
          }
        }
      }
    }
  }

  private fun <D : Operation.Data> ApolloResponse<D>.setFromCache(fromCache: Boolean): ApolloResponse<D> {
    return copy(executionContext = executionContext + CacheResponseExecutionContext(fromCache))
  }

  private fun <D : Operation.Data> writeToCache(request: ApolloRequest<D>, data: D, customScalarAdapters: CustomScalarAdapters) {
    val operation = request.operation
    val records = operation.normalize(data, customScalarAdapters, CacheKeyResolver.DEFAULT)

    store.merge(records.toList(), CacheHeaders.NONE)
  }

  private fun <D : Operation.Data> readFromCache(request: ApolloRequest<D>, customScalarAdapters: CustomScalarAdapters): ApolloResponse<D>? {
    val operation = request.operation
    val rootRecord = store.read(CacheKeyResolver.rootKey().key, CacheHeaders.NONE) ?: return null

    val fieldValueResolver = CacheValueResolver(store,
        operation.variables(),
        CacheKeyResolver.DEFAULT,
        CacheHeaders.NONE,
        RealCacheKeyBuilder()
    )
    val data = operation.parseData(rootRecord, customScalarAdapters, fieldValueResolver)

    return ApolloResponse(
        response = builder<D>(operation)
            .data(data)
            .build(),
        requestUuid = request.requestUuid,
        executionContext = request.executionContext
    ).setFromCache(true)
  }
}