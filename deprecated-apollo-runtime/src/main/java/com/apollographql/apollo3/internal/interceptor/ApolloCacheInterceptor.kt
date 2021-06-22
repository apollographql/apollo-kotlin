package com.apollographql.apollo3.internal.interceptor

import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.cache.ApolloCacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.internal.DefaultApolloStore
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloGenericException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.withCacheInfo
import com.benasher44.uuid.uuid4
import java.lang.Runnable
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking

/**
 * ApolloCacheInterceptor is a concrete [ApolloInterceptor] responsible for serving requests from the normalized
 * cache if [InterceptorRequest.fetchFromCache] is true. Saves all network responses to cache.
 */
class ApolloCacheInterceptor<D : Operation.Data>(
    val apolloStore: ApolloStore,
    private val dispatcher: Executor,
    val logger: ApolloLogger,
    private val responseCallback: AtomicReference<ApolloCall.Callback<D>?>,
    private val customScalarAdapters: CustomScalarAdapters
) : ApolloInterceptor {

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
          override fun onResponse(response: InterceptorResponse) {
            if (disposed) return
            cacheResponseAndPublish(request, response)
            callBack.onResponse(response)
            callBack.onCompleted()
          }

          override fun onFailure(e: ApolloException) {
            rollbackOptimisticUpdates(request, true)
            callBack.onFailure(e)
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
    val data = runBlocking {
      apolloStore.readOperation(
          operation = request.operation,
          cacheHeaders = request.cacheHeaders,
          customScalarAdapters = customScalarAdapters,
      )
    }
    if (data != null) {
      logger.d("Cache HIT for operation %s", request.operation.name())
      return InterceptorResponse(
          null,
          ApolloResponse(
              requestUuid = uuid4(),
              operation = request.operation,
              data = data,
          ).withCacheInfo(true)
      )
    }
    logger.d("Cache MISS for operation %s", request.operation.name())
    throw ApolloGenericException(String.format("Cache miss for operation %s", request.operation.name()))
  }

  private fun cacheResponse(
      networkResponse: InterceptorResponse,
      request: InterceptorRequest
  ): Set<String> {
    if (networkResponse.parsedResponse.isPresent
        && networkResponse.parsedResponse.get()!!.hasErrors()
        && !request.cacheHeaders.hasHeader(ApolloCacheHeaders.STORE_PARTIAL_RESPONSES)) {
      return emptySet()
    }

    val data = networkResponse.parsedResponse.get()!!.data
    return if (data != null && apolloStore is DefaultApolloStore) {
      val (records, changedKeys) = runBlocking {
        apolloStore.writeOperationWithRecords(
            request.operation as Operation<Operation.Data>,
            data,
            request.cacheHeaders,
            false, // don't publish here, it's done later
            customScalarAdapters
        )
      }
      responseCallback.get()?.onCached(records.toList())
      changedKeys
    } else {
      emptySet()
    }
  }

  fun cacheResponseAndPublish(request: InterceptorRequest, networkResponse: InterceptorResponse) {
    if (request.writeToCacheAsynchronously) {
      dispatcher.execute { cacheResponseAndPublishSynchronously(request, networkResponse) }
    } else {
      cacheResponseAndPublishSynchronously(request, networkResponse)
    }
  }

  private fun cacheResponseAndPublishSynchronously(request: InterceptorRequest, networkResponse: InterceptorResponse) {
    try {
      val networkResponseCacheKeys = cacheResponse(networkResponse, request)
      val rolledBackCacheKeys = rollbackOptimisticUpdates(request, false)

      publishCacheKeys(rolledBackCacheKeys + networkResponseCacheKeys)
    } catch (rethrow: Exception) {
      rollbackOptimisticUpdates(request, true)
      throw rethrow
    }
  }

  private fun writeOptimisticUpdatesAndPublish(request: InterceptorRequest) {
    dispatcher.execute {
      try {
        if (request.optimisticUpdates.isPresent) {
          val optimisticUpdates = request.optimisticUpdates.get()
          runBlocking {
            apolloStore.writeOptimisticUpdates(
                request.operation as Operation<Operation.Data>,
                optimisticUpdates,
                request.uniqueId,
                customScalarAdapters,
                true,
            )
          }
        }
      } catch (e: Exception) {
        logger.e(e, "failed to write operation optimistic updates, for: %s", request.operation)
      }
    }
  }

  fun rollbackOptimisticUpdates(request: InterceptorRequest, publish: Boolean): Set<String> {
    return try {
      runBlocking {
        apolloStore.rollbackOptimisticUpdates(request.uniqueId, publish)
      }
    } catch (e: Exception) {
      logger.e(e, "failed to rollback operation optimistic updates, for: %s", request.operation)
      emptySet()
    }
  }

  fun publishCacheKeys(cacheKeys: Set<String>?) {
    dispatcher.execute {
      try {
        runBlocking {
          apolloStore.publish(cacheKeys!!)
        }
      } catch (e: Exception) {
        logger.e(e, "Failed to publish cache changes")
      }
    }
  }
}
