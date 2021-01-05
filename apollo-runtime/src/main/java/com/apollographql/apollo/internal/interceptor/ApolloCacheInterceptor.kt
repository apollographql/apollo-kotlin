package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import com.apollographql.apollo.cache.ApolloCacheHeaders
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.Transaction
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import java.lang.Runnable
import java.util.ArrayList
import java.util.HashSet
import java.util.concurrent.Executor

/**
 * ApolloCacheInterceptor is a concrete [ApolloInterceptor] responsible for serving requests from the normalized
 * cache if [InterceptorRequest.fetchFromCache] is true. Saves all network responses to cache.
 */
class ApolloCacheInterceptor(
    apolloStore: ApolloStore,
    dispatcher: Executor,
    logger: ApolloLogger, writeToCacheAsynchronously: Boolean) : ApolloInterceptor {
  val apolloStore: ApolloStore
  private val dispatcher: Executor
  private val writeToCacheAsynchronously: Boolean
  val logger: ApolloLogger

  @Volatile
  var disposed = false
  override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                              dispatcher: Executor, callBack: CallBack) {
    dispatcher.execute(Runnable {
      if (disposed) return@Runnable
      if (request.fetchFromCache) {
        callBack.onFetch(FetchSourceType.CACHE)
        val cachedResponse: InterceptorResponse
        try {
          cachedResponse = resolveFromCache(request)
          callBack.onResponse(cachedResponse)
          callBack.onCompleted()
        } catch (e: ApolloException) {
          callBack.onFailure(e)
        }
      } else {
        writeOptimisticUpdatesAndPublish(request)
        chain.proceedAsync(request, dispatcher, object : CallBack {
          override fun onResponse(networkResponse: InterceptorResponse) {
            if (disposed) return
            cacheResponseAndPublish(request, networkResponse, writeToCacheAsynchronously)
            callBack.onResponse(networkResponse)
            callBack.onCompleted()
          }

          override fun onFailure(t: ApolloException) {
            rollbackOptimisticUpdatesAndPublish(request)
            callBack.onFailure(t)
          }

          override fun onCompleted() {
            // call onCompleted in onResponse
          }

          override fun onFetch(sourceType: FetchSourceType) {
            callBack.onFetch(sourceType)
          }
        })
      }
    })
  }

  override fun dispose() {
    disposed = true
  }

  @Throws(ApolloException::class)
  fun resolveFromCache(request: InterceptorRequest): InterceptorResponse {
    val responseNormalizer = apolloStore.cacheResponseNormalizer()
    val apolloStoreOperation = apolloStore.read(
        request.operation,
        responseNormalizer,
        request.cacheHeaders)
    val cachedResponse = apolloStoreOperation.execute()
    if (cachedResponse.data != null) {
      logger.d("Cache HIT for operation %s", request.operation.name().name())
      return InterceptorResponse(null, cachedResponse, responseNormalizer.records())
    }
    logger.d("Cache MISS for operation %s", request.operation.name().name())
    throw ApolloException(String.format("Cache miss for operation %s", request.operation.name().name()))
  }

  fun cacheResponse(networkResponse: InterceptorResponse,
                    request: InterceptorRequest): Set<String> {
    if (networkResponse.parsedResponse.isPresent
        && networkResponse.parsedResponse.get()!!.hasErrors()
        && !request.cacheHeaders.hasHeader(ApolloCacheHeaders.STORE_PARTIAL_RESPONSES)) {
      return emptySet()
    }
    val records = networkResponse.cacheRecords.orNull()?.let { records ->
      val result: MutableList<Record> = ArrayList(records.size)
      for (record in records) {
        result.add(record.toBuilder().mutationId(request.uniqueId).build())
      }
      result
    }
    return if (records == null) {
      emptySet()
    } else try {
      apolloStore.writeTransaction(object : Transaction<WriteableStore, Set<String>> {
        override fun execute(cache: WriteableStore): Set<String>? {
          return cache.merge(records, request.cacheHeaders)
        }
      })
    } catch (e: Exception) {
      logger.e("Failed to cache operation response", e)
      emptySet()
    }
  }

  fun cacheResponseAndPublish(request: InterceptorRequest, networkResponse: InterceptorResponse, async: Boolean) {
    if (async) {
      dispatcher.execute { cacheResponseAndPublishSynchronously(request, networkResponse) }
    } else {
      cacheResponseAndPublishSynchronously(request, networkResponse)
    }
  }

  fun cacheResponseAndPublishSynchronously(request: InterceptorRequest, networkResponse: InterceptorResponse) {
    try {
      val networkResponseCacheKeys = cacheResponse(networkResponse, request)
      val rolledBackCacheKeys = rollbackOptimisticUpdates(request)
      val changedCacheKeys: MutableSet<String> = HashSet()
      changedCacheKeys.addAll(rolledBackCacheKeys)
      changedCacheKeys.addAll(networkResponseCacheKeys)
      publishCacheKeys(changedCacheKeys)
    } catch (rethrow: Exception) {
      rollbackOptimisticUpdatesAndPublish(request)
      throw rethrow
    }
  }

  fun writeOptimisticUpdatesAndPublish(request: InterceptorRequest) {
    dispatcher.execute {
      try {
        if (request.optimisticUpdates.isPresent) {
          val optimisticUpdates = request.optimisticUpdates.get()
          apolloStore.writeOptimisticUpdatesAndPublish(request.operation as Operation<Operation.Data>, optimisticUpdates, request.uniqueId)
              .execute()
        }
      } catch (e: Exception) {
        logger.e(e, "failed to write operation optimistic updates, for: %s", request.operation)
      }
    }
  }

  fun rollbackOptimisticUpdatesAndPublish(request: InterceptorRequest) {
    dispatcher.execute {
      try {
        apolloStore.rollbackOptimisticUpdatesAndPublish(request.uniqueId).execute()
      } catch (e: Exception) {
        logger.e(e, "failed to rollback operation optimistic updates, for: %s", request.operation)
      }
    }
  }

  fun rollbackOptimisticUpdates(request: InterceptorRequest): Set<String> {
    return try {
      apolloStore.rollbackOptimisticUpdates(request.uniqueId).execute()
    } catch (e: Exception) {
      logger.e(e, "failed to rollback operation optimistic updates, for: %s", request.operation)
      emptySet()
    }
  }

  fun publishCacheKeys(cacheKeys: Set<String>?) {
    dispatcher.execute {
      try {
        apolloStore.publish(cacheKeys!!)
      } catch (e: Exception) {
        logger.e(e, "Failed to publish cache changes")
      }
    }
  }

  init {
    this.apolloStore = __checkNotNull(apolloStore, "cache == null")
    this.dispatcher = __checkNotNull(dispatcher, "dispatcher == null")
    this.logger = __checkNotNull(logger, "logger == null")
    this.writeToCacheAsynchronously = writeToCacheAsynchronously
  }
}