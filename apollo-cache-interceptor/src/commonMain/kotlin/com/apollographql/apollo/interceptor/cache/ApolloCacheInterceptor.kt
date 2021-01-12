package com.apollographql.apollo.interceptor.cache

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

@ApolloExperimental
class ApolloCacheInterceptor<S>(private val store: S) : ApolloRequestInterceptor where S : WriteableStore, S : ReadableStore {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val response = readFromCache(request, chain.customScalarAdapters)
      if (response != null) {
        emit(
            ApolloResponse(
                requestUuid = request.requestUuid,
                response = response,
                executionContext = request.executionContext + CacheExecutionContext(true)
            )
        )
      } else {
        chain.proceed(request).collect {
          if (it.response.data != null) {
            writeToCache(request, it.response.data!!, chain.customScalarAdapters)
          }
          emit(it.copy(executionContext = it.executionContext + CacheExecutionContext(false)))
        }
      }
    }
  }

  private fun <D : Operation.Data> writeToCache(request: ApolloRequest<D>, data: D, customScalarAdapters: CustomScalarAdapters) {
    val operation = request.operation
    val records = operation.normalize(data, customScalarAdapters, CacheKeyResolver.DEFAULT)

    store.merge(records.toList(), CacheHeaders.NONE)
  }

  private fun <D : Operation.Data> readFromCache(request: ApolloRequest<D>, customScalarAdapters: CustomScalarAdapters): Response<D>? {
    val operation = request.operation
    val rootRecord = store.read(CacheKeyResolver.rootKey().key, CacheHeaders.NONE) ?: return null

    val fieldValueResolver = CacheValueResolver(store,
        operation.variables(),
        CacheKeyResolver.DEFAULT,
        CacheHeaders.NONE,
        RealCacheKeyBuilder()
    )
    val data = operation.parseData(rootRecord, customScalarAdapters, fieldValueResolver)

    return builder<D>(operation)
        .data(data)
        .build()
  }
}