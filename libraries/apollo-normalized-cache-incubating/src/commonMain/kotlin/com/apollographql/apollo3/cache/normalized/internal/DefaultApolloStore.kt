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
import com.apollographql.apollo3.cache.normalized.api.EmbeddedFieldsProvider
import com.apollographql.apollo3.cache.normalized.api.FieldKeyGenerator
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
    private val fieldKeyGenerator: FieldKeyGenerator,
    private val metadataGenerator: MetadataGenerator,
    private val cacheResolver: Any,
    private val recordMerger: RecordMerger,
    private val embeddedFieldsProvider: EmbeddedFieldsProvider,
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
  private val cache: OptimisticNormalizedCache by lazy {
    OptimisticNormalizedCache(normalizedCacheFactory.create())
  }

  override suspend fun publish(keys: Set<String>) {
    if (keys.isEmpty()) {
      return
    }

    changedKeysEvents.emit(keys)
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
        fieldKeyGenerator = fieldKeyGenerator,
        embeddedFieldsProvider = embeddedFieldsProvider,
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
        variables = variables,
        fieldKeyGenerator = fieldKeyGenerator,
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
        fieldKeyGenerator = fieldKeyGenerator,
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
  ): Set<String> {
    val records = operation.normalize(
        data = operationData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator,
        metadataGenerator = metadataGenerator,
        fieldKeyGenerator = fieldKeyGenerator,
        embeddedFieldsProvider = embeddedFieldsProvider,
    ).values.toSet()

    return cache.merge(records, cacheHeaders, recordMerger)
  }

  override fun <D : Fragment.Data> writeFragment(
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
        metadataGenerator = metadataGenerator,
        fieldKeyGenerator = fieldKeyGenerator,
        embeddedFieldsProvider = embeddedFieldsProvider,
        rootKey = cacheKey.key
    ).values

    return cache.merge(records, cacheHeaders, recordMerger)
  }

  override fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      customScalarAdapters: CustomScalarAdapters,
  ): Set<String> {
    val records = operation.normalize(
        data = operationData,
        customScalarAdapters = customScalarAdapters,
        cacheKeyGenerator = cacheKeyGenerator,
        metadataGenerator = metadataGenerator,
        fieldKeyGenerator = fieldKeyGenerator,
        embeddedFieldsProvider = embeddedFieldsProvider,
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
    return cache.addOptimisticUpdates(records)
  }

  override fun rollbackOptimisticUpdates(
      mutationId: Uuid,
  ): Set<String> {
    return cache.removeOptimisticUpdates(mutationId)
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
        fieldKeyGenerator: FieldKeyGenerator,
    ): CacheData {
      return when (cacheResolver) {
        is CacheResolver -> readDataFromCacheInternal(
            cacheKey = cacheKey,
            cache = cache,
            cacheResolver = cacheResolver,
            cacheHeaders = cacheHeaders,
            variables = variables,
            fieldKeyGenerator = fieldKeyGenerator,
        )

        is ApolloResolver -> readDataFromCacheInternal(
            cacheKey = cacheKey,
            cache = cache,
            cacheResolver = cacheResolver,
            cacheHeaders = cacheHeaders,
            variables = variables,
            fieldKeyGenerator = fieldKeyGenerator,
        )

        else -> throw IllegalStateException()
      }
    }
  }
}
