package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.ClientScope
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.ExecutionParameters
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.internal.customScalarAdapters
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheInfo
import com.apollographql.apollo3.cache.normalized.FetchPolicy
import com.apollographql.apollo3.cache.normalized.cacheHeaders
import com.apollographql.apollo3.cache.normalized.dependentKeys
import com.apollographql.apollo3.cache.normalized.doNotStore
import com.apollographql.apollo3.cache.normalized.fetchPolicy
import com.apollographql.apollo3.cache.normalized.isFromCache
import com.apollographql.apollo3.cache.normalized.optimisticData
import com.apollographql.apollo3.cache.normalized.refetchPolicy
import com.apollographql.apollo3.cache.normalized.storePartialResponses
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.cache.normalized.withCacheInfo
import com.apollographql.apollo3.cache.normalized.writeToCacheAsynchronously
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.CacheMissException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.mpp.currentTimeMillis
import com.apollographql.apollo3.mpp.ensureNeverFrozen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch

internal class ApolloCacheInterceptor(
    val store: ApolloStore,
) : ApolloInterceptor {
  init {
    // The store has a MutableSharedFlow that doesn't like being frozen
    ensureNeverFrozen(store)
  }

  private val <T> ExecutionParameters<T>.clientScope: CoroutineScope where T : ExecutionParameters<T>
    get() = executionContext[ClientScope]!!.coroutineScope

  private suspend fun <D : Operation.Data> maybeAsync(request: ApolloRequest<D>, block: suspend () -> Unit) {
    if (request.writeToCacheAsynchronously) {
      request.clientScope.launch { block() }
    } else {
      block()
    }
  }

  /**
   * @param extraKeys extra keys to publish in case we has optimistic data
   */
  private suspend fun <D : Operation.Data> maybeWriteToCache(
      request: ApolloRequest<D>,
      response: ApolloResponse<D>,
      customScalarAdapters: CustomScalarAdapters,
      extraKeys: Set<String> = emptySet(),
  ) {
    if (request.doNotStore) {
      return
    }
    if (response.hasErrors() && !request.storePartialResponses) {
      return
    }

    maybeAsync(request) {
      val cacheKeys = if (response.data != null) {
        store.writeOperation(request.operation, response.data!!, customScalarAdapters, request.cacheHeaders, publish = false)
      } else {
        emptySet()
      }
      store.publish(cacheKeys + extraKeys)
    }
  }

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return when (request.operation) {
      is Subscription -> {
        // That's a lot of unchecked casts but should be always true
        @Suppress("UNCHECKED_CAST")
        interceptSubscription(request as ApolloRequest<Subscription.Data>, chain) as Flow<ApolloResponse<D>>
      }
      is Mutation -> {
        // That's a lot of unchecked casts but should be always true
        @Suppress("UNCHECKED_CAST")
        interceptMutation(request as ApolloRequest<Mutation.Data>, chain) as Flow<ApolloResponse<D>>
      }
      is Query -> {
        // That's a lot of unchecked casts but should be always true
        @Suppress("UNCHECKED_CAST")
        interceptQuery(request as ApolloRequest<Query.Data>, chain) as Flow<ApolloResponse<D>>
      }
      else -> error("Unknown operation ${request.operation}")
    }
  }

  /**
   * Subscriptions  always go to the network
   */
  private fun <D : Subscription.Data> interceptSubscription(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain,
  ): Flow<ApolloResponse<D>> {
    return readFromNetwork(request, chain, request.customScalarAdapters)
  }

  /**
   * Mutations always go to the network and support optimistic data
   */
  private fun <D : Mutation.Data> interceptMutation(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val customScalarAdapters = request.customScalarAdapters

    return flow {
      val optimisticData = request.optimisticData
      if (optimisticData != null) {
        @Suppress("UNCHECKED_CAST")
        store.writeOptimisticUpdates(
            operation = request.operation,
            operationData = optimisticData as D,
            mutationId = request.requestUuid,
            customScalarAdapters = customScalarAdapters,
            publish = true
        )
      }

      /**
       * This doesn't use [readFromNetwork] so that we can publish all keys all at once after the keys have been rolled back
       */
      val response = chain.proceed(request).single()

      val optimisticKeys = if (optimisticData != null) {
        store.rollbackOptimisticUpdates(request.requestUuid, publish = false)
      } else {
        emptySet()
      }

      maybeWriteToCache(request, response, customScalarAdapters, optimisticKeys)
      emit(response)
    }
  }


  private fun <D : Query.Data> interceptQuery(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val fetchPolicy = request.fetchPolicy
    val refetchPolicy = request.refetchPolicy
    val customScalarAdapters = request.customScalarAdapters
    return flow {
      var result = kotlin.runCatching {
        fetchOneMightThrow(request, chain, fetchPolicy, customScalarAdapters)
      }
      val response = result.getOrNull()

      if (response != null) {
        emit(response)
      }

      if (!request.watch) {
        if (result.isFailure) {
          throw result.exceptionOrNull()!!
        }
        return@flow
      }

      var watchedKeys = if (response != null && !response.hasErrors() && response.data != null) {
        store.normalize(request.operation, response.data!!, customScalarAdapters).values.dependentKeys()
      } else {
        null
      }

      store.changedKeys.collect { changedKeys ->
        if (watchedKeys == null || changedKeys.intersect(watchedKeys!!).isNotEmpty()) {
          result = kotlin.runCatching {
            fetchOneMightThrow(request, chain, refetchPolicy, customScalarAdapters)
          }

          val newResponse = result.getOrNull()
          if (newResponse != null) {
            emit(newResponse)

            if (!newResponse.hasErrors() && newResponse.data != null) {
              watchedKeys = store.normalize(request.operation, newResponse.data!!, customScalarAdapters).values.dependentKeys()
            }
          }
        }
      }
    }
  }

  private suspend fun <D : Query.Data> fetchOneMightThrow(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain,
      fetchPolicy: FetchPolicy,
      customScalarAdapters: CustomScalarAdapters,
  ): ApolloResponse<D> {
    when (fetchPolicy) {
      FetchPolicy.CacheFirst -> {
        val cacheResult = readFromCache(request, customScalarAdapters)

        if (cacheResult.response != null) {
          return cacheResult.response.withCacheInfo(cacheResult.cacheInfo)
        }

        val networkResult = kotlin.runCatching {
          readOneFromNetwork(request, chain, customScalarAdapters)
        }

        val networkResponse = networkResult.getOrNull()
        if (networkResponse != null) {
          return networkResponse.withCacheInfo(cacheResult.cacheInfo)
        }

        throw ApolloCompositeException(
            cacheResult.error,
            networkResult.exceptionOrNull()
        )
      }
      FetchPolicy.NetworkFirst -> {
        val networkResult = kotlin.runCatching {
          readOneFromNetwork(request, chain, customScalarAdapters)
        }

        val networkResponse = networkResult.getOrNull()
        if (networkResponse != null) {
          return networkResponse
        }

        val cacheResult = readFromCache(request, customScalarAdapters)
        if (cacheResult.response != null) {
          return cacheResult.response.withCacheInfo(cacheResult.cacheInfo)
        }

        throw ApolloCompositeException(
            networkResult.exceptionOrNull(),
            cacheResult.error,
        )
      }
      FetchPolicy.CacheOnly -> {
        val cacheResult = readFromCache(request, customScalarAdapters)
        if (cacheResult.response != null) {
          return cacheResult.response.withCacheInfo(cacheResult.cacheInfo)
        }
        throw cacheResult.error!!
      }
      FetchPolicy.NetworkOnly -> {
        return readOneFromNetwork(request, chain, customScalarAdapters)
      }
    }
  }

  private class CacheResult<D : Query.Data>(
      val response: ApolloResponse<D>?,
      val cacheInfo: CacheInfo,
      val error: Throwable?,
  )

  private suspend fun <D : Query.Data> readFromCache(
      request: ApolloRequest<D>,
      customScalarAdapters: CustomScalarAdapters,
  ): CacheResult<D> {
    val operation = request.operation
    val millisStart = currentTimeMillis()

    val result = kotlin.runCatching {
      store.readOperation(
          operation = operation,
          customScalarAdapters = customScalarAdapters,
          cacheHeaders = request.cacheHeaders
      )
    }

    val response = if (result.isSuccess) {
      ApolloResponse(
          requestUuid = request.requestUuid,
          operation = operation,
          data = result.getOrThrow(),
          executionContext = request.executionContext
      )
    } else {
      null
    }

    val cacheMissException = result.exceptionOrNull() as? CacheMissException

    return CacheResult(
        response = response,
        cacheInfo = CacheInfo(
            millisStart = millisStart,
            millisEnd = currentTimeMillis(),
            hit = response != null,
            missedKey = cacheMissException?.key,
            missedField = cacheMissException?.fieldName
        ),
        error = result.exceptionOrNull()
    )
  }

  private fun <D : Operation.Data> readFromNetwork(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain,
      customScalarAdapters: CustomScalarAdapters,
  ): Flow<ApolloResponse<D>> {
    return chain.proceed(request).onEach {
      maybeWriteToCache(request, it, customScalarAdapters)
    }
  }

  private suspend fun <D : Operation.Data> readOneFromNetwork(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain,
      customScalarAdapters: CustomScalarAdapters,
  ): ApolloResponse<D> = readFromNetwork(request, chain, customScalarAdapters).single()
}
