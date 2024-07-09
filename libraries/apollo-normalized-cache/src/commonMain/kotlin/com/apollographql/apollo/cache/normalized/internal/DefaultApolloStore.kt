package com.apollographql.apollo.cache.normalized.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.variables
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.api.CacheHeaders
import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo.cache.normalized.api.CacheResolver
import com.apollographql.apollo.cache.normalized.api.NormalizedCache
import com.apollographql.apollo.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.api.Record
import com.apollographql.apollo.cache.normalized.api.internal.OptimisticCache
import com.apollographql.apollo.cache.normalized.api.normalize
import com.apollographql.apollo.cache.normalized.api.readDataFromCacheInternal
import com.apollographql.apollo.cache.normalized.api.toData
import com.benasher44.uuid.Uuid
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.reflect.KClass

internal class DefaultApolloStore(
    normalizedCacheFactory: NormalizedCacheFactory,
    private val cacheKeyGenerator: CacheKeyGenerator,
    private val cacheResolver: CacheResolver,
) : ApolloStore {
  private val changedKeysEvents = MutableSharedFlow<Set<String>>(
      /**
       * The '64' magic number here is a potential code smell
       *
       * If a watcher is very slow to collect, cache writes continue buffering changedKeys events until the buffer is full.
       * If that ever happens, this is probably an issue in the calling code, and we currently log that to the user. A more
       * advanced version of this code could also expose the buffer size to the caller for better control.
       *
       * Also, we have had issues before where one or several watchers would loop forever, creating useless network requests.
       * There is unfortunately very little evidence of how it could happen, but I could reproduce under the following conditions:
       * 1. A field that returns ever-changing values (think current time for an example)
       * 2. A refetch policy that uses the network ([NetworkOnly] or [CacheFirst] do for an example)
       *
       * In that case, a single watcher will trigger itself endlessly.
       *
       * My current understanding is that here as well, the fix is probably best done at the callsite by not using [NetworkOnly]
       * as a refetchPolicy. If that ever becomes an issue again, please make sure to write a test about it.
       */
      extraBufferCapacity = 64,
      onBufferOverflow = BufferOverflow.SUSPEND
  )

  override val changedKeys = changedKeysEvents.asSharedFlow()

  // Keeping this as lazy to avoid accessing the disk at initialization which usually happens on the main thread
  private val cache: OptimisticCache by lazy {
    OptimisticCache().chain(normalizedCacheFactory.createChain()) as OptimisticCache
  }

  private val lock = Lock()

  override suspend fun publish(keys: Set<String>) {
    if (keys.isEmpty()) {
      return
    }

    changedKeysEvents.emit(keys)
  }

  override fun clearAll(): Boolean {
    lock.write {
      cache.clearAll()
    }
    return true
  }

  override fun remove(
      cacheKey: CacheKey,
      cascade: Boolean,
  ): Boolean {
    return lock.write {
      cache.remove(cacheKey, cascade)
    }
  }

  override fun remove(
      cacheKeys: List<CacheKey>,
      cascade: Boolean,
  ): Int {
    return lock.write {
      var count = 0
      for (cacheKey in cacheKeys) {
        if (cache.remove(cacheKey, cascade = cascade)) {
          count++
        }
      }
      count
    }
  }

  override fun <D : Operation.Data> normalize(
      operation: Operation<D>,
      data: D,
      customScalarAdapters: CustomScalarAdapters,
  ): Map<String, Record> {
    return operation.normalize(
        data,
        customScalarAdapters,
        cacheKeyGenerator
    )
  }

  override fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D {
    val variables = operation.variables(customScalarAdapters, true)
    return lock.read {
      operation.readDataFromCacheInternal(
          cache = cache,
          cacheResolver = cacheResolver,
          cacheHeaders = cacheHeaders,
          variables = variables
      )
    }.toData(operation.adapter(), customScalarAdapters, variables)
  }

  override fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D {
    val variables = fragment.variables(customScalarAdapters, true)
    return lock.read {
      fragment.readDataFromCacheInternal(
          cache = cache,
          cacheResolver = cacheResolver,
          cacheHeaders = cacheHeaders,
          cacheKey = cacheKey,
          variables = variables,
      )
    }.toData(fragment.adapter(), customScalarAdapters, variables)
  }

  override fun <R> accessCache(block: (NormalizedCache) -> R): R {
    /**
     * We don't know how the cache is going to be used, assume write access
     */
    return lock.write { block(cache) }
  }

  override suspend fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    val changedKeys =  writeOperationSync(
        operation = operation,
        operationData = operationData,
        cacheHeaders = cacheHeaders,
        customScalarAdapters = customScalarAdapters
    )

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  override fun <D : Operation.Data> writeOperationSync(operation: Operation<D>, operationData: D, customScalarAdapters: CustomScalarAdapters, cacheHeaders: CacheHeaders): Set<String> {
    val records = operation.normalize(
        data = operationData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator
    ).values

    val changedKeys = lock.write {
      cache.merge(records, cacheHeaders)
    }

    return changedKeys
  }

  override suspend fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    val changedKeys = writeFragmentSync(fragment, cacheKey, fragmentData, customScalarAdapters, cacheHeaders)

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  override fun <D : Fragment.Data> writeFragmentSync(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): Set<String> {
    val records = fragment.normalize(
        data = fragmentData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator,
        rootKey = cacheKey.key
    ).values

    val changedKeys = lock.write {
      cache.merge(records, cacheHeaders)
    }

    return changedKeys
  }

  override suspend fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      customScalarAdapters: CustomScalarAdapters,
      publish: Boolean,
  ): Set<String> {
    val changedKeys = writeOptimisticUpdatesSync(operation, operationData, mutationId, customScalarAdapters)

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  override fun <D : Operation.Data> writeOptimisticUpdatesSync(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      customScalarAdapters: CustomScalarAdapters,
  ): Set<String> {
    val records = operation.normalize(
        data = operationData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator
    ).values.map { record ->
      Record(
          key = record.key,
          fields = record.fields,
          mutationId = mutationId
      )
    }
    val changedKeys = lock.write {
      /**
       * TODO: should we forward the cache headers to the optimistic store?
       */
      cache.addOptimisticUpdates(records)
    }

    return changedKeys
  }

  override suspend fun rollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean,
  ): Set<String> {
    val changedKeys = rollbackOptimisticUpdatesSync(mutationId)

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  override fun rollbackOptimisticUpdatesSync(
      mutationId: Uuid,
  ): Set<String> {
    val changedKeys = lock.write {
      cache.removeOptimisticUpdates(mutationId)
    }

    return changedKeys
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return lock.read {
      cache.dump()
    }
  }

  override fun dispose() {}
}

