package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Executable
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.variables
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.api.ApolloResolver
import com.apollographql.apollo3.cache.normalized.api.CacheData
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
import com.apollographql.apollo3.cache.normalized.api.internal.OptimisticNormalizedCache
import com.apollographql.apollo3.cache.normalized.api.normalize
import com.apollographql.apollo3.cache.normalized.api.readDataFromCacheInternal
import com.apollographql.apollo3.cache.normalized.api.toData
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

  // Keeping this as lazy to avoid accessing the disk at initialization which usually happens on the main thread
  private val cache: OptimisticNormalizedCache by lazy {
    OptimisticNormalizedCache(normalizedCacheFactory.create())
  }

  override fun publish(keys: Set<String>) {
    if (keys.isEmpty()) {
      return
    }

    changedKeysEvents.tryEmit(keys)
  }

  override fun clearAll(): Boolean {
    cache.clearAll()
    return true
  }

  override fun remove(
      cacheKey: CacheKey,
      cascade: Boolean,
  ): Boolean {
    return cache.remove(cacheKey, cascade)
  }

  override fun remove(
      cacheKeys: List<CacheKey>,
      cascade: Boolean,
  ): Int {
    var count = 0
    for (cacheKey in cacheKeys) {
      if (cache.remove(cacheKey, cascade = cascade)) {
        count++
      }
    }
    return count
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

  override fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D {
    val variables = operation.variables(customScalarAdapters, true)
    return operation.readDataFromCachePrivate(
        cache = cache,
        cacheResolver = cacheResolver,
        cacheHeaders = cacheHeaders,
        cacheKey = CacheKey.rootKey(),
        variables = variables
    ).toData(operation.adapter(), customScalarAdapters, variables)
  }

  override fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D {
    val variables = fragment.variables(customScalarAdapters, true)

    return fragment.readDataFromCachePrivate(
        cache = cache,
        cacheResolver = cacheResolver,
        cacheHeaders = cacheHeaders,
        cacheKey = cacheKey,
        variables = variables,
    ).toData(fragment.adapter(), customScalarAdapters, variables)
  }

  override fun <R> accessCache(block: (NormalizedCache) -> R): R {
    return block(cache)
  }

  override fun <D : Operation.Data> writeOperation(
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

  override fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    val records = fragment.normalize(
        data = fragmentData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator,
        metadataGenerator = metadataGenerator,
        rootKey = cacheKey.key
    ).values

    val changedKeys = cache.merge(records, cacheHeaders, recordMerger)
    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  private fun <D : Operation.Data> writeOperationWithRecords(
      operation: Operation<D>,
      operationData: D,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
      customScalarAdapters: CustomScalarAdapters,
  ): Pair<Set<Record>, Set<String>> {
    val records = operation.normalize(
        data = operationData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator,
        metadataGenerator = metadataGenerator,
    ).values.toSet()

    val changedKeys = cache.merge(records, cacheHeaders, recordMerger)
    if (publish) {
      publish(changedKeys)
    }

    return records to changedKeys
  }


  override fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      customScalarAdapters: CustomScalarAdapters,
      publish: Boolean,
  ): Set<String> {
    val records = operation.normalize(
        data = operationData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator,
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
    val changedKeys = cache.addOptimisticUpdates(records)
    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  override fun rollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean,
  ): Set<String> {
    val changedKeys = cache.removeOptimisticUpdates(mutationId)
    if (publish) {
      publish(changedKeys)
    }

    return changedKeys
  }

  fun merge(record: Record, cacheHeaders: CacheHeaders): Set<String> {
    return cache.merge(record, cacheHeaders, recordMerger)
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return cache.dump()
  }

  override fun dispose() {}

  companion object {
    private fun <D : Executable.Data> Executable<D>.readDataFromCachePrivate(
        cacheKey: CacheKey,
        cache: ReadOnlyNormalizedCache,
        cacheResolver: Any,
        cacheHeaders: CacheHeaders,
        variables: Executable.Variables,
    ): CacheData {
      return when (cacheResolver) {
        is CacheResolver -> readDataFromCacheInternal(
            cacheKey,
            cache,
            cacheResolver,
            cacheHeaders,
            variables
        )

        is ApolloResolver -> readDataFromCacheInternal(
            cacheKey,
            cache,
            cacheResolver,
            cacheHeaders,
            variables
        )

        else -> throw IllegalStateException()
      }
    }
  }
}
