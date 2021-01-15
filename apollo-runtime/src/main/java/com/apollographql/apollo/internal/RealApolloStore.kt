package com.apollographql.apollo.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.ApolloStore.RecordChangeSubscriber
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.OptimisticNormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.cache.normalized.internal.readDataFromCache
import java.util.Collections
import java.util.LinkedHashSet
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock

class RealApolloStore(normalizedCache: NormalizedCache,
                      private val cacheKeyResolver: CacheKeyResolver,
                      val customScalarAdapters: CustomScalarAdapters,
                      val logger: ApolloLogger) : ApolloStore, ReadableStore, WriteableStore {
  private val optimisticCache = OptimisticNormalizedCache().chain(normalizedCache) as OptimisticNormalizedCache
  private val lock = ReentrantReadWriteLock()
  private val subscribers: MutableSet<RecordChangeSubscriber> = Collections.newSetFromMap(WeakHashMap())

  @Synchronized
  override fun subscribe(subscriber: RecordChangeSubscriber) {
    subscribers.add(subscriber)
  }

  @Synchronized
  override fun unsubscribe(subscriber: RecordChangeSubscriber) {
    subscribers.remove(subscriber)
  }

  override fun publish(keys: Set<String>) {
    if (keys.isEmpty()) {
      return
    }
    var iterableSubscribers: Set<RecordChangeSubscriber>
    synchronized(this) { iterableSubscribers = LinkedHashSet(subscribers) }
    for (subscriber in iterableSubscribers) {
      subscriber.onCacheRecordsChanged(keys)
    }
  }

  override fun clearAll(): Boolean {
    return writeTransaction {
      optimisticCache.clearAll()
      true
    }
  }

  override fun remove(
      cacheKey: CacheKey,
      cascade: Boolean
  ) = writeTransaction { optimisticCache.remove(cacheKey, cascade) }

  override fun remove(
      cacheKeys: List<CacheKey>,
      cascade: Boolean
  ): Int {
    return writeTransaction {
      var count = 0
      for (cacheKey in cacheKeys) {
        if (optimisticCache.remove(cacheKey)) {
          count++
        }
      }
      count
    }
  }

  /**
   * not private because tests use it
   */
  fun <R> readTransaction(block: (ReadableStore) -> R): R {
    lock.readLock().lock()
    return try {
      block(this@RealApolloStore)
    } finally {
      lock.readLock().unlock()
    }
  }

  /**
   * not private because tests use it
   */
  fun <R> writeTransaction(block: (WriteableStore) -> R): R {
    lock.writeLock().lock()
    return try {
      block(this@RealApolloStore)
    } finally {
      lock.writeLock().unlock()
    }
  }

  override fun normalizedCache(): NormalizedCache {
    return optimisticCache
  }

  override fun read(key: String, cacheHeaders: CacheHeaders): Record? {
    return optimisticCache.loadRecord(key, cacheHeaders)
  }

  override fun stream(key: String, cacheHeaders: CacheHeaders): JsonReader? {
    return optimisticCache.stream(key, cacheHeaders)
  }

  override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return optimisticCache.loadRecords(keys, cacheHeaders)
  }

  override fun merge(recordCollection: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    return optimisticCache.merge(recordCollection, cacheHeaders)
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return optimisticCache.merge(record, cacheHeaders)
  }

  override fun cacheKeyResolver(): CacheKeyResolver {
    return cacheKeyResolver
  }

  override fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      cacheHeaders: CacheHeaders
  ): D? {
    return readTransaction { cache ->
      try {
        operation.readDataFromCache(
            customScalarAdapters,
            cache,
            cacheKeyResolver(),
            cacheHeaders
        )
      } catch (e: Exception) {
        logger.e(e, "Failed to read cache response")
        null
      }
    }
  }

  override fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      cacheHeaders: CacheHeaders
  ): D? {
    return readTransaction { cache ->
      try {
        fragment.readDataFromCache(
            customScalarAdapters,
            cache,
            cacheKeyResolver(),
            cacheHeaders,
            cacheKey
        )
      } catch (e: Exception) {
        logger.e(e, "Failed to read cache response")
        null
      }
    }
  }

  override fun <D : Operation.Data> writeOperationWithRecords(
      operation: Operation<D>,
      operationData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean
  ): Pair<Set<Record>, Set<String>> {
    val records = operation.normalize(operationData, customScalarAdapters, cacheKeyResolver)
    val changedKeys = optimisticCache.merge(records, cacheHeaders)

    if (publish) {
      publish(changedKeys)
    }
    return records to changedKeys
  }

  override fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean
  ) = writeOperationWithRecords(operation, operationData, cacheHeaders, publish).second

  override fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean
  ): Set<String> {

    require(cacheKey != CacheKey.NO_KEY) { "ApolloGraphQL: writing a fragment requires a valid cache key" }

    return writeTransaction {
      val records = fragment.normalize(fragmentData, customScalarAdapters, cacheKeyResolver, cacheKey.key)
      val changedKeys = merge(records, cacheHeaders)
      if (publish) {
        publish(changedKeys)
      }
      changedKeys
    }
  }

  override fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>, operationData: D,
      mutationId: UUID,
      publish: Boolean
  ): Set<String> {
    val records = operation.normalize(operationData, customScalarAdapters, cacheKeyResolver).map {
      it.toBuilder().mutationId(mutationId).build()
    }
    /**
     * TODO: should we forward the cache headers to the optimistic store?
     */
    val changedKeys = optimisticCache.mergeOptimisticUpdates(records)
    if (publish) {
      publish(changedKeys)
    }
    return changedKeys
  }

  override fun rollbackOptimisticUpdates(
      mutationId: UUID,
      publish: Boolean
  ): Set<String> {
    val changedKeys = writeTransaction { optimisticCache.removeOptimisticUpdates(mutationId) }
    if (publish) {
      publish(changedKeys)
    }
    return changedKeys
  }
}