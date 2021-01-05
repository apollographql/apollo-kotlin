package com.apollographql.apollo.internal

import com.apollographql.apollo.api.Adaptable
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.Response.Companion.builder
import com.apollographql.apollo.api.ResponseField
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.RealResponseReader
import com.apollographql.apollo.api.internal.ResolveDelegate
import com.apollographql.apollo.api.internal.ResponseAdapter
import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.cache.normalized.ApolloStore.RecordChangeSubscriber
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation
import com.apollographql.apollo.cache.normalized.CacheKey
import com.apollographql.apollo.cache.normalized.CacheKeyResolver
import com.apollographql.apollo.cache.normalized.CacheKeyResolver.Companion.rootKeyForOperation
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.cache.normalized.OptimisticNormalizedCache
import com.apollographql.apollo.cache.normalized.Record
import com.apollographql.apollo.cache.normalized.internal.CacheFieldValueResolver
import com.apollographql.apollo.cache.normalized.internal.CacheKeyBuilder
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.RealCacheKeyBuilder
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer
import com.apollographql.apollo.cache.normalized.internal.Transaction
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import com.apollographql.apollo.internal.response.RealResponseWriter
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashSet
import java.util.UUID
import java.util.WeakHashMap
import java.util.concurrent.Executor
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock

class RealApolloStore(normalizedCache: NormalizedCache, cacheKeyResolver: CacheKeyResolver,
                      customScalarAdapters: CustomScalarAdapters, dispatcher: Executor,
                      logger: ApolloLogger) : ApolloStore, ReadableStore, WriteableStore {
  val optimisticCache: OptimisticNormalizedCache
  val cacheKeyResolver: CacheKeyResolver
  val customScalarAdapters: CustomScalarAdapters
  private val lock: ReadWriteLock
  private val subscribers: MutableSet<RecordChangeSubscriber>
  private val dispatcher: Executor
  private val cacheKeyBuilder: CacheKeyBuilder
  val logger: ApolloLogger
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

  override fun cacheResponseNormalizer(): ResponseNormalizer<Record> {
    return object : ResponseNormalizer<Record>() {
      override fun resolveCacheKey(field: ResponseField, record: Record): CacheKey {
        return CacheKey(record.key)
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
    __checkNotNull(changedKeys, "changedKeys == null")
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
    __checkNotNull(cacheKey, "cacheKey == null")
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
    __checkNotNull(cacheKeys, "cacheKey == null")
    return object : ApolloStoreOperation<Int>(dispatcher) {
      override fun perform(): Int {
        return writeTransaction(object : Transaction<WriteableStore, Int> {
          override fun execute(cache: WriteableStore): Int {
            var count = 0
            for (cacheKey in cacheKeys) {
              if (optimisticCache.remove(cacheKey)) {
                count++
              }
            }
            return count
          }
        })
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
    return optimisticCache.loadRecord(__checkNotNull(key, "key == null"), cacheHeaders)
  }

  override fun read(keys: Collection<String>, cacheHeaders: CacheHeaders): Collection<Record> {
    return optimisticCache.loadRecords(__checkNotNull(keys, "keys == null"), cacheHeaders)
  }

  override fun merge(recordSet: Collection<Record>, cacheHeaders: CacheHeaders): Set<String> {
    return optimisticCache.merge(__checkNotNull(recordSet, "recordSet == null"), cacheHeaders)
  }

  override fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return optimisticCache.merge(__checkNotNull(record, "record == null"), cacheHeaders)
  }

  override fun cacheKeyResolver(): CacheKeyResolver {
    return cacheKeyResolver
  }

  override fun <D : Operation.Data> read(
      operation: Operation<D>): ApolloStoreOperation<D> {
    __checkNotNull(operation, "operation == null")
    return object : ApolloStoreOperation<D>(dispatcher) {
      override fun perform(): D {
        return doRead(operation)
      }
    }
  }

  override fun <D : Operation.Data> read(
      operation: Operation<D>,
      responseNormalizer: ResponseNormalizer<Record>,
      cacheHeaders: CacheHeaders): ApolloStoreOperation<Response<D>> {
    __checkNotNull(operation, "operation == null")
    __checkNotNull(responseNormalizer, "responseNormalizer == null")
    return object : ApolloStoreOperation<Response<D>>(dispatcher) {
      override fun perform(): Response<D> {
        return doRead(operation, responseNormalizer, cacheHeaders)
      }
    }
  }

  override fun <F> read(
      adapter: ResponseAdapter<F>,
      cacheKey: CacheKey,
      variables: Operation.Variables): ApolloStoreOperation<F> {
    __checkNotNull(cacheKey, "cacheKey == null")
    __checkNotNull(variables, "variables == null")
    return object : ApolloStoreOperation<F>(dispatcher) {
      override fun perform(): F {
        return doRead(adapter, cacheKey, variables)
      }
    }
  }

  override fun <D : Operation.Data> write(operation: Operation<D>, operationData: D): ApolloStoreOperation<Set<String>> {
    __checkNotNull(operation, "operation == null")
    __checkNotNull(operationData, "operationData == null")
    return object : ApolloStoreOperation<Set<String>>(dispatcher) {
      override fun perform(): Set<String> {
        return doWrite(operation, operationData, false, null)
      }
    }
  }

  override fun <D : Operation.Data> writeAndPublish(operation: Operation<D>, operationData: D): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      override fun perform(): Boolean {
        val changedKeys = doWrite(operation, operationData, false, null)
        publish(changedKeys)
        return java.lang.Boolean.TRUE
      }
    }
  }

  override fun write(adaptable: Adaptable<*>,
                     cacheKey: CacheKey, variables: Operation.Variables): ApolloStoreOperation<Set<String>> {
    __checkNotNull(adaptable, "adaptable == null")
    __checkNotNull(cacheKey, "cacheKey == null")
    __checkNotNull(variables, "operation == null")
    require(!cacheKey.equals(CacheKey.NO_KEY)) { "undefined cache key" }
    return object : ApolloStoreOperation<Set<String>>(dispatcher) {
      override fun perform(): Set<String> {
        return writeTransaction(object : Transaction<WriteableStore, Set<String>> {
          override fun execute(cache: WriteableStore): Set<String>? {
            return doWrite(adaptable, cacheKey, variables)
          }
        })
      }
    }
  }

  override fun writeAndPublish(adaptable: Adaptable<*>,
                               cacheKey: CacheKey, variables: Operation.Variables): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      override fun perform(): Boolean {
        val changedKeys = doWrite(adaptable, cacheKey, variables)
        publish(changedKeys)
        return java.lang.Boolean.TRUE
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
        return writeTransaction(object : Transaction<WriteableStore, Set<String>> {
          override fun execute(cache: WriteableStore): Set<String>? {
            return optimisticCache.removeOptimisticUpdates(mutationId)
          }
        })
      }
    }
  }

  override fun rollbackOptimisticUpdatesAndPublish(mutationId: UUID): ApolloStoreOperation<Boolean> {
    return object : ApolloStoreOperation<Boolean>(dispatcher) {
      override fun perform(): Boolean {
        val changedKeys = writeTransaction<Set<String>>(object : Transaction<WriteableStore, Set<String>> {
          override fun execute(cache: WriteableStore): Set<String>? {
            return optimisticCache.removeOptimisticUpdates(mutationId)
          }
        })
        publish(changedKeys)
        return java.lang.Boolean.TRUE
      }
    }
  }

  fun <D : Operation.Data> doRead(operation: Operation<D>): D {
    return readTransaction(object : Transaction<ReadableStore, D> {
      override fun execute(cache: ReadableStore): D? {
        val rootRecord = cache.read(rootKeyForOperation(operation).key, CacheHeaders.NONE) ?: return null
        val fieldValueResolver = CacheFieldValueResolver(
            cache,
            operation.variables(),
            cacheKeyResolver(),
            CacheHeaders.NONE,
            cacheKeyBuilder)
        val responseReader = RealResponseReader<Record>(operation.variables(), rootRecord,
            fieldValueResolver, customScalarAdapters, ResponseNormalizer.NO_OP_NORMALIZER as ResolveDelegate<Record>)
        return operation.adapter().fromResponse(responseReader, null)
      }
    })
  }

  fun <D : Operation.Data> doRead(
      operation: Operation<D>,
      responseNormalizer: ResponseNormalizer<Record>,
      cacheHeaders: CacheHeaders): Response<D> {
    return readTransaction(object : Transaction<ReadableStore, Response<D>> {
      override fun execute(cache: ReadableStore): Response<D> {
        val rootRecord = cache.read(rootKeyForOperation(operation).key, cacheHeaders)
            ?: return builder<D>(operation).fromCache(true).build()
        val fieldValueResolver = CacheFieldValueResolver(
            cache,
            operation.variables(),
            cacheKeyResolver(),
            cacheHeaders,
            cacheKeyBuilder)
        val responseReader = RealResponseReader(operation.variables(), rootRecord,
            fieldValueResolver, customScalarAdapters, responseNormalizer)
        return try {
          responseNormalizer.willResolveRootQuery(operation)
          val data = operation.adapter().fromResponse(responseReader, null)
          builder<D>(operation)
              .data(data)
              .fromCache(true)
              .dependentKeys(responseNormalizer.dependentKeys())
              .build()
        } catch (e: Exception) {
          logger.e(e, "Failed to read cache response")
          builder<D>(operation).fromCache(true).build()
        }
      }
    })
  }

  fun <F> doRead(adapter: ResponseAdapter<F>,
                 cacheKey: CacheKey, variables: Operation.Variables): F {
    return readTransaction(object : Transaction<ReadableStore, F> {
      override fun execute(cache: ReadableStore): F? {
        val rootRecord = cache.read(cacheKey.key, CacheHeaders.NONE) ?: return null
        val fieldValueResolver = CacheFieldValueResolver(cache, variables,
            cacheKeyResolver(), CacheHeaders.NONE, cacheKeyBuilder)
        val responseReader = RealResponseReader<Record>(variables, rootRecord,
            fieldValueResolver, customScalarAdapters, ResponseNormalizer.NO_OP_NORMALIZER as ResolveDelegate<Record>)
        return adapter.fromResponse(responseReader, null)
      }
    })
  }

  fun <D : Operation.Data> doWrite(
      operation: Operation<D>, operationData: D, optimistic: Boolean,
      mutationId: UUID?): Set<String> {
    return writeTransaction(object : Transaction<WriteableStore, Set<String>> {
      override fun execute(cache: WriteableStore): Set<String>? {
        val responseWriter = RealResponseWriter(operation.variables(), customScalarAdapters)
        operation.adapter().toResponse(responseWriter, operationData)
        val responseNormalizer = networkResponseNormalizer()
        responseNormalizer.willResolveRootQuery(operation)
        responseWriter.resolveFields(responseNormalizer as ResolveDelegate<Map<String, Any>?>)
        return if (optimistic) {
          val updatedRecords: MutableList<Record> = ArrayList()
          for (record in responseNormalizer.records()) {
            updatedRecords.add(record.toBuilder().mutationId(mutationId).build())
          }
          optimisticCache.mergeOptimisticUpdates(updatedRecords)
        } else {
          optimisticCache.merge(responseNormalizer.records(), CacheHeaders.NONE)
        }
      }
    })
  }

  fun doWrite(adaptable: Adaptable<*>, cacheKey: CacheKey, variables: Operation.Variables): Set<String> {
    return writeTransaction(object : Transaction<WriteableStore, Set<String>> {
      override fun execute(cache: WriteableStore): Set<String>? {
        val responseWriter = RealResponseWriter(variables, customScalarAdapters)
        (adaptable as Adaptable<Any>).adapter().toResponse(responseWriter, adaptable)
        val responseNormalizer = networkResponseNormalizer()
        responseNormalizer.willResolveRecord(cacheKey)
        responseWriter.resolveFields(responseNormalizer as ResolveDelegate<Map<String, Any>?>)
        return merge(responseNormalizer.records(), CacheHeaders.NONE)
      }
    })
  }

  init {
    __checkNotNull(normalizedCache, "cacheStore == null")
    optimisticCache = OptimisticNormalizedCache().chain(normalizedCache) as OptimisticNormalizedCache
    this.cacheKeyResolver = __checkNotNull(cacheKeyResolver, "cacheKeyResolver == null")
    this.customScalarAdapters = __checkNotNull(customScalarAdapters, "scalarTypeAdapters == null")
    this.dispatcher = __checkNotNull(dispatcher, "dispatcher == null")
    this.logger = __checkNotNull(logger, "logger == null")
    lock = ReentrantReadWriteLock()
    subscribers = Collections.newSetFromMap(WeakHashMap())
    cacheKeyBuilder = RealCacheKeyBuilder()
  }
}