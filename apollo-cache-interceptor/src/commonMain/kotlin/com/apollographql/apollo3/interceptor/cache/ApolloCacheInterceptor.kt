package com.apollographql.apollo3.interceptor.cache

import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.Platform
import com.apollographql.apollo3.cache.normalized.internal.dependentKeys
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.interceptor.ApolloRequestInterceptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single

@OptIn(ExperimentalCoroutinesApi::class)
class ApolloCacheInterceptor(private val store: ApolloStore) : ApolloRequestInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val cacheInput = request.executionContext[FetchPolicyContext]
    val fetchPolicy = cacheInput?.fetchPolicy ?: FetchPolicy.CacheFirst
    val refetchPolicy = request.executionContext[RefetchPolicyContext]?.refetchPolicy

    if (refetchPolicy == null) {
      return flow {
        emit(fetchOne(request, chain, fetchPolicy))
      }
    }
    return callbackFlow {
      var result = kotlin.runCatching {
        fetchOne(request, chain, fetchPolicy)
      }
      val response = result.getOrNull()
      result.exceptionOrNull()?.printStackTrace()

      if (response != null) {
        offer(response)
      }

      var watchedKeys = if (response != null && !response.hasErrors() && response.data != null) {
        store.normalize(request.operation, response.data!!, chain.responseAdapterCache).values.dependentKeys()
      } else {
        null
      }

      store.changedKeys.collect { changedKeys ->
        if (watchedKeys == null || changedKeys.intersect(watchedKeys!!).isNotEmpty()) {
          result = kotlin.runCatching {
            fetchOne(request, chain, refetchPolicy)
          }

          val newResponse = result.getOrNull()
          if (newResponse != null) {
            offer(newResponse)

            if (!newResponse.hasErrors() && newResponse.data != null) {
              watchedKeys = store.normalize(request.operation, newResponse .data!!, chain.responseAdapterCache).values.dependentKeys()
            }
          }
        }
      }
    }
  }

  private suspend fun <D : Operation.Data> fetchOne(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain,
      fetchPolicy: FetchPolicy
  ): ApolloResponse<D> {
    when (fetchPolicy) {
      FetchPolicy.CacheFirst -> {
                Platform.ensureNeverFrozen(store)
        val cacheResult = kotlin.runCatching {
          readFromCache(request, chain.responseAdapterCache)
        }

        val cacheResponse = cacheResult.getOrNull()
        if (cacheResponse != null) {
          return cacheResponse
        }

        val networkResult = kotlin.runCatching {
          proceed(request, chain).single()
        }

        val networkResponse = networkResult.getOrNull()
        if (networkResponse != null) {
          return networkResponse
        }

        throw ApolloCompositeException(
            cacheResult.exceptionOrNull(),
            networkResult.exceptionOrNull()
        )
      }
      FetchPolicy.NetworkFirst -> {
        val networkResult = kotlin.runCatching {
          proceed(request, chain).single()
        }

        val networkResponse = networkResult.getOrNull()
        if (networkResponse != null) {
          return networkResponse
        }

        val cacheResult = kotlin.runCatching {
          readFromCache(request, chain.responseAdapterCache)
        }

        val cacheResponse = cacheResult.getOrNull()
        if (cacheResponse != null) {
          return cacheResponse
        }

        throw ApolloCompositeException(
            networkResult.exceptionOrNull(),
            cacheResult.exceptionOrNull(),
        )
      }
      FetchPolicy.CacheOnly -> {
        return readFromCache(request, chain.responseAdapterCache)
      }
      FetchPolicy.NetworkOnly -> {
        return proceed(request, chain).single()
      }
    }
  }

  private fun <D : Operation.Data> proceed(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val responseAdapterCache  = chain.responseAdapterCache
        return chain.proceed(request).map {
      if (it.data != null) {
        writeToCache(request, it.data!!, responseAdapterCache)
      }
      it.setFromCache(false)
    }
  }

  private fun <D : Operation.Data> ApolloResponse<D>.setFromCache(fromCache: Boolean): ApolloResponse<D> {
    return copy(executionContext = executionContext + CacheOutput(fromCache))
  }

  private suspend fun <D : Operation.Data> writeToCache(
      request: ApolloRequest<D>,
      data: D,
      responseAdapterCache: ResponseAdapterCache,
  ): Set<String> {

    return store.writeOperation(request.operation, data, responseAdapterCache, CacheHeaders.NONE, true)
  }

  private suspend fun <D : Operation.Data> readFromCache(
      request: ApolloRequest<D>,
      responseAdapterCache: ResponseAdapterCache,
  ): ApolloResponse<D> {
    val operation = request.operation

    val data = store.readOperation(
        operation,
        responseAdapterCache
    )

    return ApolloResponse(
        requestUuid = request.requestUuid,
        operation = operation,
        data = data,
        executionContext = request.executionContext + CacheOutput(true)
    )
  }
}
