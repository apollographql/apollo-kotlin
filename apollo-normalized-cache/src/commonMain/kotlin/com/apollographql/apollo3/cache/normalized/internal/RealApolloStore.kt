package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.ApolloInternal
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.Record
import com.benasher44.uuid.Uuid
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlin.reflect.KClass

class RealApolloStore(
    normalizedCacheFactory: NormalizedCacheFactory,
    private val cacheKeyResolver: CacheKeyResolver,
    val logger: ApolloLogger = ApolloLogger(null)
) : ApolloStore() {
  private val subscribersLock = reentrantLock()
  private val subscribers = mutableSetOf<RecordChangeSubscriber>()

  private val cacheHolder = DefaultCacheHolder {
    OptimisticCache().chain(normalizedCacheFactory.createChain()) as OptimisticCache
  }

  override fun subscribe(subscriber: RecordChangeSubscriber) {
    subscribersLock.withLock {
      subscribers.add(subscriber)
    }
  }

  override fun unsubscribe(subscriber: RecordChangeSubscriber) {
    subscribersLock.withLock {
      subscribers.remove(subscriber)
    }
  }

  override fun publish(keys: Set<String>) {
    if (keys.isEmpty()) {
      return
    }

    val subscribers = subscribersLock.withLock {
      subscribers.toList()
    }

    subscribers.forEach { subscriber ->
      subscriber.onCacheRecordsChanged(keys)
    }
  }

  override fun clearAll(): Boolean {
    cacheHolder.writeAndForget {
      it.clearAll()
    }
    return true
  }

  override suspend fun remove(
      cacheKey: CacheKey,
      cascade: Boolean
  ): Boolean {
    return cacheHolder.write {
      it.remove(cacheKey, cascade)
    }
  }

  override suspend fun remove(
      cacheKeys: List<CacheKey>,
      cascade: Boolean
  ): Int {
    return cacheHolder.write {
      var count = 0
      for (cacheKey in cacheKeys) {
        if (it.remove(cacheKey, cascade = cascade)) {
          count++
        }
      }
      count
    }
  }

  override suspend fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      responseAdapterCache: ResponseAdapterCache,
      cacheHeaders: CacheHeaders,
      mode: ReadMode,
  ): D? {
    return cacheHolder.read { cache ->
      try {
        operation.readDataFromCache(
            responseAdapterCache = responseAdapterCache,
            cache = cache,
            cacheKeyResolver = cacheKeyResolver,
            cacheHeaders = cacheHeaders,
            mode = mode,
        )
      } catch (e: Exception) {
        logger.e(e, "Failed to read cache response")
        null
      }
    }
  }

  override suspend fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      responseAdapterCache: ResponseAdapterCache,
      cacheHeaders: CacheHeaders,
  ): D? {
    return cacheHolder.read { cache ->
      try {
        fragment.readDataFromCache(
            responseAdapterCache = responseAdapterCache,
            cache = cache,
            cacheKeyResolver = cacheKeyResolver,
            cacheHeaders = cacheHeaders,
            cacheKey = cacheKey
        )
      } catch (e: Exception) {
        logger.e(e, "Failed to read cache response")
        null
      }
    }
  }

  @OptIn(ApolloInternal::class)
  override suspend fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      responseAdapterCache: ResponseAdapterCache,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    return writeOperationWithRecords(
        operation = operation,
        operationData = operationData,
        cacheHeaders = cacheHeaders,
        publish = publish,
        responseAdapterCache = responseAdapterCache
    ).second
  }

  override suspend fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      responseAdapterCache: ResponseAdapterCache,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    require(cacheKey != CacheKey.NO_KEY) {
      "ApolloGraphQL: writing a fragment requires a valid cache key"
    }

    return cacheHolder.write { cache ->
      val records = fragment.normalize(
          data = fragmentData,
          responseAdapterCache = responseAdapterCache,
          cacheKeyResolver = cacheKeyResolver,
          rootKey = cacheKey.key
      ).values

      val changedKeys = cache.merge(records, cacheHeaders)
      if (publish) {
        publish(changedKeys)
      }

      changedKeys
    }
  }

  suspend fun <D : Operation.Data> writeOperationWithRecords(
      operation: Operation<D>,
      operationData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
      responseAdapterCache: ResponseAdapterCache,
  ): Pair<Set<Record>, Set<String>> {
    val (records, changedKeys) = cacheHolder.write { cache ->
      val records = operation.normalize(
          data = operationData,
          responseAdapterCache = responseAdapterCache,
          cacheKeyResolver = cacheKeyResolver
      )

      records to cache.merge(records.values.toList(), cacheHeaders)
    }
    if (publish) {
      publish(changedKeys)
    }

    return records.values.toSet() to changedKeys
  }

  override suspend fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      responseAdapterCache: ResponseAdapterCache,
      publish: Boolean,
  ): Set<String> {
    val changedKeys = cacheHolder.write { cache ->
      val records = operation.normalize(
          data = operationData,
          responseAdapterCache = responseAdapterCache,
          cacheKeyResolver = cacheKeyResolver
      ).values.map { record ->
        Record(
            key = record.key,
            fields = record.fields,
            mutationId = mutationId
        )
      }

      /**
       * TODO: should we forward the cache headers to the optimistic store?
       */
      cache.mergeOptimisticUpdates(records)
    }

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  override suspend fun rollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean
  ): Set<String> {
    val changedKeys = cacheHolder.write { cache ->
      cache.removeOptimisticUpdates(mutationId)
    }

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  suspend fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return cacheHolder.write { cache ->
      cache.merge(record, cacheHeaders)
    }
  }

  override suspend fun dump(): Map<KClass<*>, Map<String, Record>> {
    return cacheHolder.read { cache ->
      cache.dump()
    }
  }
}

fun ApolloStore(normalizedCacheFactory: NormalizedCacheFactory,
                cacheKeyResolver: CacheKeyResolver): ApolloStore = RealApolloStore(normalizedCacheFactory, cacheKeyResolver)