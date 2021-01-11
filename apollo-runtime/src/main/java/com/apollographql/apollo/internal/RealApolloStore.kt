package com.apollographql.apollo.internal

import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.Response.Companion.builder
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.MapResponseReader
import com.apollographql.apollo.api.internal.ResolveDelegate
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.ApolloStore.RecordChangeSubscriber
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.CacheKeyResolver.Companion.rootKey
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.OptimisticNormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.CacheValueResolver
import com.apollographql.apollo.cache.normalized.internal.CacheKeyBuilder
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.RealCacheKeyBuilder
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer
import com.apollographql.apollo.cache.normalized.internal.Transaction
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import com.apollographql.apollo.api.internal.response.RealResponseWriter
import com.apollographql.apollo.api.parseData
import com.apollographql.apollo.cache.normalized.internal.dependentKeys
import com.apollographql.apollo.cache.normalized.internal.normalize
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashSet
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReentrantReadWriteLock

class RealApolloStore(normalizedCache: NormalizedCache,
                      val cacheKeyResolver: CacheKeyResolver,
                      val customScalarAdapters: CustomScalarAdapters,
                      private val dispatcher: Executor,
                      val logger: ApolloLogger) : ApolloStore, ReadableStore, WriteableStore {
  private val optimisticCache = OptimisticNormalizedCache().chain(normalizedCache) as OptimisticNormalizedCache
  private val lock = ReentrantReadWriteLock()
  private val subscribers : MutableSet<RecordChangeSubscriber> = Collections.newSetFromMap(WeakHashMap())
  private val cacheKeyBuilder = RealCacheKeyBuilder()

  override fun networkResponseNormalizer(): ResponseNormalizer<Map<String, Any>> {
    return object : ResponseNormalizer<Map<String, Any>>() {
      override fun resolveCacheKey(field: ResponseField,
                                   record: Map<String, Any>): CacheKey {
        return cacheKeyResolver.fromFieldRecordSet(field, record)
      }

      override fun cacheKeyBuilder(): CacheKeyBuilder {
        return cacheKeyBuilder
      }
    }
  }

  @Synchronized
  override fun subscribe(subscriber: RecordChangeSubscriber) {
    subscribers.add(subscriber)
  }

  @Synchronized
  override fun unsubscribe(subscriber: RecordChangeSubscriber) {
    subscribers.remove(subscriber)
  }

  override fun publish(changedKeys: Set<String>) {
    if (changedKeys.isEmpty()) {
      return
    }
    var iterableSubscribers: Set<RecordChangeSubscriber>
    synchronized(this) { iterableSubscribers = LinkedHashSet(subscribers) }
    for (subscriber in iterableSubscribers) {
      subscriber.onCacheRecordsChanged(changedKeys)
    }
  }

  override fun clearAll(): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      public override fun perform(): Boolean {
        return writeTransaction(object : Transaction<WriteableStore, Boolean> {
          override fun execute(cache: WriteableStore): Boolean {
            optimisticCache.clearAll()
            return java.lang.Boolean.TRUE
          }
        })
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
        return writeTransaction(object : Transaction<WriteableStore, Boolean> {
          override fun execute(cache: WriteableStore): Boolean {
            return optimisticCache.remove(cacheKey, cascade)
          }
        })
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

  override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return optimisticCache.loadRecords(keys, cacheHeaders)
  }

  override fun merge(recordSet: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    return optimisticCache.merge(recordSet, cacheHeaders)
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return optimisticCache.merge(record, cacheHeaders)
  }

  override fun cacheKeyResolver(): CacheKeyResolver {
    return cacheKeyResolver
  }

  override fun <D : Operation.Data> readOperation(
      operation: Operation<D>): ApolloStoreOperation<D> {
    return object : ApolloStoreOperation<D>(dispatcher) {
      override fun perform(): D {
        return doReadAndNormalize(operation, CacheHeaders.NONE).data!!
      }
    }
  }

  override fun <D : Operation.Data> readOperationInternal(
      operation: Operation<D>,
      cacheHeaders: CacheHeaders): ApolloStoreOperation<Response<D>> {
    return object : ApolloStoreOperation<Response<D>>(dispatcher) {
      override fun perform(): Response<D> {
        return doReadAndNormalize(operation, cacheHeaders)
      }
    }
  }

  override fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
  ): ApolloStoreOperation<D> {
    return object : ApolloStoreOperation<D>(dispatcher) {
      override fun perform(): D {
        return doRead(fragment.adapter(), cacheKey, fragment.variables())
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
        val changedKeys = doWrite(operation, operationData, false, null)
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
          val changedKeys = doWrite(fragment.adapter(), cacheKey, fragment.variables(), fragmentData)
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
        return doWrite(operation, operationData, true, mutationId)
      }
    }
  }

  override fun <D : Operation.Data> writeOptimisticUpdatesAndPublish(operation: Operation<D>, operationData: D,
                                                                     mutationId: UUID): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      override fun perform(): Boolean {
        val changedKeys = doWrite(operation, operationData, true, mutationId)
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

  fun <D : Operation.Data> doReadAndNormalize(
      operation: Operation<D>,
      cacheHeaders: CacheHeaders
  ): Response<D> = readTransaction { cache ->
    val rootRecord = cache.read(rootKey().key, cacheHeaders)
        ?: return@readTransaction builder<D>(operation).fromCache(true).build()
    try {
      val fieldValueResolver = CacheValueResolver(
          cache,
          operation.variables(),
          cacheKeyResolver(),
          cacheHeaders,
          cacheKeyBuilder)
      val data = operation.parseData(rootRecord, customScalarAdapters, fieldValueResolver)
      val records = operation.normalize(data, customScalarAdapters, cacheKeyResolver)
      builder<D>(operation)
          .data(data)
          .fromCache(true)
          .dependentKeys(records.dependentKeys()) // Do we need the dependentKeys here?
          .build()
    } catch (e: Exception) {
      logger.e(e, "Failed to read cache response")
      builder<D>(operation).fromCache(true).build()
    }
  }


  fun <F> doRead(adapter: ResponseAdapter<F>,
                 cacheKey: CacheKey,
                 variables: Operation.Variables): F {
    return readTransaction(object : Transaction<ReadableStore, F> {
      override fun execute(cache: ReadableStore): F? {
        val rootRecord = cache.read(cacheKey.key, CacheHeaders.NONE) ?: return null
        val fieldValueResolver = CacheValueResolver(cache, variables,
            cacheKeyResolver(), CacheHeaders.NONE, cacheKeyBuilder)
        val responseReader = MapResponseReader(
            variables,
            rootRecord,
            fieldValueResolver,
            customScalarAdapters,
        )
        return adapter.fromResponse(responseReader, null)
      }
    })
  }

  fun <D : Operation.Data> doWrite(
      operation: Operation<D>,
      operationData: D,
      optimistic: Boolean,
      mutationId: UUID?): Set<String> = writeTransaction {
    val responseWriter = RealResponseWriter(operation.variables(), customScalarAdapters)
    operation.adapter().toResponse(responseWriter, operationData)
    val responseNormalizer = networkResponseNormalizer()
    responseNormalizer.willResolveRootQuery(operation)
    responseWriter.resolveFields(responseNormalizer as ResolveDelegate<Map<String, Any>?>)
    if (optimistic) {
      val updatedRecords: MutableList<Record> = ArrayList()
      for (record in responseNormalizer.records()) {
        updatedRecords.add(record.toBuilder().mutationId(mutationId).build())
      }
      optimisticCache.mergeOptimisticUpdates(updatedRecords)
    } else {
      optimisticCache.merge(responseNormalizer.records(), CacheHeaders.NONE)
    }

  }

  fun <D> doWrite(adapter: ResponseAdapter<D>, cacheKey: CacheKey, variables: Operation.Variables, value: D): Set<String> = writeTransaction {
      val responseWriter = RealResponseWriter(variables, customScalarAdapters)
      adapter.toResponse(responseWriter, value)
      val responseNormalizer = networkResponseNormalizer()
      responseNormalizer.willResolveRecord(cacheKey)
      responseWriter.resolveFields(responseNormalizer as ResolveDelegate<Map<String, Any>?>)
      merge(responseNormalizer.records(), CacheHeaders.NONE)
    }

}