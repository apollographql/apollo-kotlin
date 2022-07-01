package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.ApolloResolver
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.ReadOnlyNormalizedCache
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.apollographql.apollo3.cache.normalized.api.internal.OptimisticCache
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.cache.normalized.api.readDataFromCache
import com.benasher44.uuid.Uuid
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlin.reflect.KClass

internal class DefaultApolloStore(
    normalizedCacheFactory: NormalizedCacheFactory,
    private val cacheKeyGenerator: CacheKeyGenerator,
    private val metadataGenerator: MetadataGenerator,
    private val cacheResolver: Any,
    private val recordMerger: RecordMerger,
) : ApolloStore {
  private val changedKeysEvents = MutableSharedFlow<Set<String>>(
      // XXX: this is a potential code smell
      // If multiple watchers start notifying each other and potentially themselves, the buffer of changedKeysEvent will grow forever.
      // I think as long as the refetchPolicy is [FetchPolicy.CacheOnly] everything should be fine as there is no reentrant emission.
      // If the refetechPolicy is something else, we should certainly try to detect it in the cache interceptor
      extraBufferCapacity = 10,
      onBufferOverflow = BufferOverflow.DROP_OLDEST
  )

  override val changedKeys = changedKeysEvents.asSharedFlow()

  private val cacheHolder = Guard("OptimisticCache") {
    OptimisticCache().chain(normalizedCacheFactory.createChain()) as OptimisticCache
  }

  override suspend fun publish(keys: Set<String>) {
    if (keys.isEmpty()) {
      return
    }

    changedKeysEvents.emit(keys)
  }

  override fun clearAll(): Boolean {
    cacheHolder.writeAndForget {
      it.clearAll()
    }
    return true
  }

  override suspend fun remove(
      cacheKey: CacheKey,
      cascade: Boolean,
  ): Boolean {
    return cacheHolder.writeAccess {
      it.remove(cacheKey, cascade)
    }
  }

  override suspend fun remove(
      cacheKeys: List<CacheKey>,
      cascade: Boolean,
  ): Int {
    return cacheHolder.writeAccess {
      var count = 0
      for (cacheKey in cacheKeys) {
        if (it.remove(cacheKey, cascade = cascade)) {
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
        data = data,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator,
        metadataGenerator = metadataGenerator,
    )
  }

  override suspend fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D {
    // Capture a local reference so as not to freeze "this"
    val cacheResolver = cacheResolver

    return cacheHolder.readAccess { cache ->
      operation.readDataFromCacheInternal(
          customScalarAdapters = customScalarAdapters,
          cache = cache,
          cacheResolver = cacheResolver,
          cacheHeaders = cacheHeaders,
          cacheKey = CacheKey.rootKey()
      )
    }
  }

  override suspend fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D {
    // Capture a local reference so as not to freeze "this"
    val cacheResolver = cacheResolver

    return cacheHolder.readAccess { cache ->
      fragment.readDataFromCacheInternal(
          customScalarAdapters = customScalarAdapters,
          cache = cache,
          cacheResolver = cacheResolver,
          cacheHeaders = cacheHeaders,
          cacheKey = cacheKey
      )
    }
  }


  override suspend fun <R> accessCache(block: (NormalizedCache) -> R): R {
    /**
     * We don't know how the cache is going to be used, assume write access
     */
    return cacheHolder.writeAccess(block)
  }

  override suspend fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    return writeOperationWithRecords(
        operation = operation,
        operationData = operationData,
        cacheHeaders = cacheHeaders,
        publish = publish,
        customScalarAdapters = customScalarAdapters
    ).second
  }

  override suspend fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    // Capture a local reference so as not to freeze "this"
    val objectIdGenerator = cacheKeyGenerator
    val recordMerger = recordMerger
    val metadataGenerator = metadataGenerator

    val changedKeys = cacheHolder.writeAccess { cache ->
      val records = fragment.normalize(
          data = fragmentData,
          customScalarAdapters = customScalarAdapters,
          cacheKeyGenerator = objectIdGenerator,
          metadataGenerator = metadataGenerator,
          rootKey = cacheKey.key
      ).values

      cache.merge(records, cacheHeaders, recordMerger)
    }

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  suspend fun <D : Operation.Data> writeOperationWithRecords(
      operation: Operation<D>,
      operationData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
      customScalarAdapters: CustomScalarAdapters,
  ): Pair<Set<Record>, Set<String>> {

    // Capture a local reference so as not to freeze "this"
    val objectIdGenerator = cacheKeyGenerator
    val recordMerger = recordMerger
    val metadataGenerator = metadataGenerator

    val (records, changedKeys) = cacheHolder.writeAccess { cache ->
      val records = operation.normalize(
          data = operationData,
          customScalarAdapters = customScalarAdapters,
          cacheKeyGenerator = objectIdGenerator,
          metadataGenerator = metadataGenerator,
      )

      records to cache.merge(records.values.toList(), cacheHeaders, recordMerger)
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
      customScalarAdapters: CustomScalarAdapters,
      publish: Boolean,
  ): Set<String> {

    // Capture a local reference so as not to freeze "this"
    val objectIdGenerator = cacheKeyGenerator
    val metadataGenerator = metadataGenerator

    val changedKeys = cacheHolder.writeAccess { cache ->
      val records = operation.normalize(
          data = operationData,
          customScalarAdapters = customScalarAdapters,
          cacheKeyGenerator = objectIdGenerator,
          metadataGenerator = metadataGenerator,
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
      cache.addOptimisticUpdates(records)
    }

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  override suspend fun rollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean,
  ): Set<String> {
    val changedKeys = cacheHolder.writeAccess { cache ->
      cache.removeOptimisticUpdates(mutationId)
    }

    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  suspend fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return cacheHolder.writeAccess { cache ->
      cache.merge(record, cacheHeaders, recordMerger)
    }
  }

  override suspend fun dump(): Map<KClass<*>, Map<String, Record>> {
    return cacheHolder.readAccess { cache ->
      cache.dump()
    }
  }

  override fun dispose() {
    cacheHolder.dispose()
  }

  companion object {
    private fun <D : Executable.Data> Executable<D>.readDataFromCacheInternal(
        cacheKey: CacheKey,
        customScalarAdapters: CustomScalarAdapters,
        cache: ReadOnlyNormalizedCache,
        cacheResolver: Any,
        cacheHeaders: CacheHeaders,
    ): D {
      return when (cacheResolver) {
        is CacheResolver -> readDataFromCache(
            cacheKey,
            customScalarAdapters,
            cache,
            cacheResolver,
            cacheHeaders
        )
        is ApolloResolver -> readDataFromCache(
            cacheKey,
            customScalarAdapters,
            cache,
            cacheResolver,
            cacheHeaders
        )
        else -> throw IllegalStateException()
      }
    }
  }
}

