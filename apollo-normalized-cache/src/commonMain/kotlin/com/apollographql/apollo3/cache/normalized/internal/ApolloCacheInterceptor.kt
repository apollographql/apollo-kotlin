package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.ConcurrencyInfo
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheInfo
import com.apollographql.apollo3.cache.normalized.cacheHeaders
import com.apollographql.apollo3.cache.normalized.cacheInfo
import com.apollographql.apollo3.cache.normalized.doNotStore
import com.apollographql.apollo3.cache.normalized.fetchFromCache
import com.apollographql.apollo3.cache.normalized.optimisticData
import com.apollographql.apollo3.cache.normalized.storePartialResponses
import com.apollographql.apollo3.cache.normalized.writeToCacheAsynchronously
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.mpp.currentTimeMillis
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch

internal class ApolloCacheInterceptor(
    val store: ApolloStore,
) : ApolloInterceptor {
  init {
    // The store has a MutableSharedFlow that doesn't like being frozen when using coroutines
    // But is ok to freeze when using coroutines-native-mt (see https://github.com/apollographql/apollo-android/issues/3357)
    // ensureNeverFrozen(store)
  }

  private suspend fun <D : Operation.Data> maybeAsync(request: ApolloRequest<D>, block: suspend () -> Unit) {
    if (request.writeToCacheAsynchronously) {
      val scope = request.executionContext[ConcurrencyInfo]!!.coroutineScope
      scope.launch { block() }
    } else {
      block()
    }
  }

  /**
   * @param extraKeys extra keys to publish in case there is optimistic data
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
    }.flowOn(request.executionContext[ConcurrencyInfo]!!.dispatcher)
  }

  /**
   * Subscriptions always go to the network
   */
  private fun <D : Subscription.Data> interceptSubscription(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain,
  ): Flow<ApolloResponse<D>> {
    val customScalarAdapters = request.customScalarAdapters

    return chain.proceed(request).onEach {
      maybeWriteToCache(request, it, customScalarAdapters)
    }
  }

  val <D : Operation.Data> ApolloRequest<D>.customScalarAdapters: CustomScalarAdapters
    get() = executionContext[CustomScalarAdapters]!!

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
      var response: ApolloResponse<D>? = null
      var exception: ApolloException? = null
      try {
        response = chain.proceed(request).single()
      } catch (e: ApolloException) {
        exception = e
      }

      val optimisticKeys = if (optimisticData != null) {
        store.rollbackOptimisticUpdates(request.requestUuid, publish = false)
      } else {
        emptySet()
      }

      if (response != null) {
        maybeWriteToCache(request, response, customScalarAdapters, optimisticKeys)
        emit(response)
      } else {
        store.publish(optimisticKeys)
        throw exception!!
      }
    }
  }

  private fun <D : Query.Data> interceptQuery(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val customScalarAdapters = request.customScalarAdapters
    val fetchFromCache = request.fetchFromCache

    return flow {
      emit(
          if (fetchFromCache) {
            readFromCache(request, customScalarAdapters)
          } else {
            readFromNetwork(request, chain, customScalarAdapters)
          }
      )
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
  ): ApolloResponse<D> {
    val operation = request.operation
    val startMillis = currentTimeMillis()

    val data = store.readOperation(
        operation = operation,
        customScalarAdapters = customScalarAdapters,
        cacheHeaders = request.cacheHeaders
    )

    return ApolloResponse.Builder(
        requestUuid = request.requestUuid,
        operation = operation,
        data = data,
    ).addExecutionContext(request.executionContext)
        .cacheInfo(
            CacheInfo.Builder()
                .cacheStartMillis(startMillis)
                .cacheEndMillis(currentTimeMillis())
                .build()
        )
        .build()
  }


  private suspend fun <D : Operation.Data> readFromNetwork(
      request: ApolloRequest<D>,
      chain: ApolloInterceptorChain,
      customScalarAdapters: CustomScalarAdapters,
  ): ApolloResponse<D> {
    val startMillis = currentTimeMillis()
    val networkResponse = chain.proceed(request).onEach {
      maybeWriteToCache(request, it, customScalarAdapters)
    }.single()

    return networkResponse.newBuilder()
        .cacheInfo(
            CacheInfo.Builder()
                .networkStartMillis(startMillis)
                .networkEndMillis(currentTimeMillis())
                .build()
        ).build()
  }
}
