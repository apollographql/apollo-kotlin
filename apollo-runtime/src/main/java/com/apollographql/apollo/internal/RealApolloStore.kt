package com.apollographql.apollo.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.json.JsonReader
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.ApolloStore.RecordChangeSubscriber
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.OptimisticNormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.Transaction
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import com.apollographql.apollo.cache.normalized.internal.normalize
import com.apollographql.apollo.cache.normalized.internal.readDataFromCache
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashSet
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantReadWriteLock

class RealApolloStore(normalizedCache: NormalizedCache,
                      private val cacheKeyResolver: CacheKeyResolver,
                      val customScalarAdapters: CustomScalarAdapters,
                      private val dispatcher: Executor,
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

  override fun clearAll(): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      public override fun perform(): Boolean {
        return writeTransaction {
          optimisticCache.clearAll()
          java.lang.Boolean.TRUE
        }
      }
    }
  }

  override fun remove(cacheKey: CacheKey): ApolloStoreOperation<Boolean> {
    return remove(cacheKey, false)
  }

  override fun remove(cacheKey: CacheKey,
                      cascade: Boolean): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      override fun perform(): Boolean {
        return writeTransaction { optimisticCache.remove(cacheKey, cascade) }
      }
    }
  }

  override fun remove(cacheKeys: List<CacheKey>): ApolloStoreOperation<Int> {
    return object : ApolloStoreOperation<Int>(dispatcher) {
      override fun perform(): Int {
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
    }
  }

  override fun <R> readTransaction(transaction: Transaction<ReadableStore, R>): R {
    lock.readLock().lock()
    return try {
      transaction.execute(this@RealApolloStore)!!
    } finally {
      lock.readLock().unlock()
    }
  }

  override fun <R> writeTransaction(transaction: Transaction<WriteableStore, R>): R {
    lock.writeLock().lock()
    return try {
      transaction.execute(this@RealApolloStore)!!
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
  ): ApolloStoreOperation<D> {
    return object : ApolloStoreOperation<D>(dispatcher) {
      override fun perform(): D {
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
    }
  }

  override fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      cacheHeaders: CacheHeaders
  ): ApolloStoreOperation<D> {
    return object : ApolloStoreOperation<D>(dispatcher) {
      override fun perform(): D {
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
    }
  }

  override fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      publish: Boolean
  ): ApolloStoreOperation<Set<String>> {
    return object : ApolloStoreOperation<Set<String>>(dispatcher) {
      override fun perform(): Set<String> {
        val changedKeys = doWriteOperation(operation, operationData, false, null)
        if (publish) {
          publish(changedKeys)
        }
        return changedKeys
      }
    }
  }

  override fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      publish: Boolean
  ): ApolloStoreOperation<Set<String>> {

    require(cacheKey != CacheKey.NO_KEY) { "undefined cache key" }

    return object : ApolloStoreOperation<Set<String>>(dispatcher) {
      override fun perform(): Set<String> {
        return writeTransaction {
          val records = fragment.normalize(fragmentData, customScalarAdapters, cacheKeyResolver, cacheKey.key)
          val changedKeys = merge(records, CacheHeaders.NONE)
          if (publish) {
            publish(changedKeys)
          }
          changedKeys
        }
      }
    }
  }

  override fun <D : Operation.Data> writeOptimisticUpdates(operation: Operation<D>, operationData: D,
                                                           mutationId: UUID): ApolloStoreOperation<Set<String>> {
    return object : ApolloStoreOperation<Set<String>>(dispatcher) {
      override fun perform(): Set<String> {
        return doWriteOperation(operation, operationData, true, mutationId)
      }
    }
  }

  override fun <D : Operation.Data> writeOptimisticUpdatesAndPublish(operation: Operation<D>, operationData: D,
                                                                     mutationId: UUID): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      override fun perform(): Boolean {
        val changedKeys = doWriteOperation(operation, operationData, true, mutationId)
        publish(changedKeys)
        return java.lang.Boolean.TRUE
      }
    }
  }

  override fun rollbackOptimisticUpdates(mutationId: UUID): ApolloStoreOperation<Set<String>> {
    return object : ApolloStoreOperation<Set<String>>(dispatcher) {
      override fun perform(): Set<String> {
        return writeTransaction { optimisticCache.removeOptimisticUpdates(mutationId) }
      }
    }
  }

  override fun rollbackOptimisticUpdatesAndPublish(mutationId: UUID): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      override fun perform(): Boolean {
        val changedKeys = writeTransaction { optimisticCache.removeOptimisticUpdates(mutationId) }
        publish(changedKeys)
        return java.lang.Boolean.TRUE
      }
    }
  }


  fun <D : Operation.Data> doWriteOperation(
      operation: Operation<D>,
      operationData: D,
      optimistic: Boolean,
      mutationId: UUID?): Set<String> = writeTransaction {
    val records = operation.normalize(operationData, customScalarAdapters, cacheKeyResolver)
    if (optimistic) {
      val updatedRecords: MutableList<Record> = ArrayList()
      for (record in records) {
        updatedRecords.add(record.toBuilder().mutationId(mutationId).build())
      }
      optimisticCache.mergeOptimisticUpdates(updatedRecords)
    } else {
      optimisticCache.merge(records, CacheHeaders.NONE)
    }
  }
}