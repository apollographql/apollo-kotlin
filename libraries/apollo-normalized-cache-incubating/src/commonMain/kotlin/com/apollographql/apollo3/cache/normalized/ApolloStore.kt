package com.apollographql.apollo3.cache.normalized

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.api.ApolloResolver
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.DefaultEmbeddedFieldsProvider
import com.apollographql.apollo3.cache.normalized.api.DefaultFieldKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.DefaultRecordMerger
import com.apollographql.apollo3.cache.normalized.api.EmbeddedFieldsProvider
import com.apollographql.apollo3.cache.normalized.api.EmptyMetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.FieldKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyApolloResolver
import com.apollographql.apollo3.cache.normalized.api.FieldPolicyCacheResolver
import com.apollographql.apollo3.cache.normalized.api.MetadataGenerator
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.cache.normalized.api.RecordMerger
import com.apollographql.apollo3.cache.normalized.api.TypePolicyCacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.internal.DefaultApolloStore
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.benasher44.uuid.Uuid
import kotlinx.coroutines.flow.SharedFlow
import kotlin.reflect.KClass

/**
 * ApolloStore exposes a thread-safe api to access a [com.apollographql.apollo3.cache.normalized.api.NormalizedCache].
 *
 * Note that most operations are synchronous and might block if the underlying cache is doing IO - calling them from the main thread
 * should be avoided.
 */
interface ApolloStore {
  /**
   * Exposes the keys of records that have changed.
   */
  val changedKeys: SharedFlow<Set<String>>

  /**
   * Read GraphQL operation from store.
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param operation to be read
   *
   * @throws [com.apollographql.apollo3.exception.CacheMissException] on cache miss
   * @throws [com.apollographql.apollo3.exception.ApolloException] on other cache read errors
   *
   * @return the operation data
   */
  fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): D

  /**
   * Read a GraphQL fragment from the store.
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param fragment to be read
   * @param cacheKey    [CacheKey] to be used to find cache record for the fragment
   *
   * @throws [com.apollographql.apollo3.exception.CacheMissException] on cache miss
   * @throws [com.apollographql.apollo3.exception.ApolloException] on other cache read errors
   *
   * @return the fragment data
   */
  fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): D

  /**
   * Write an operation data to the store.
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param operation     [Operation] response data of which should be written to the store
   * @param operationData [Operation.Data] operation response data to be written to the store
   * @return the changed keys
   *
   * @see publish
   */
  fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): Set<String>

  /**
   * Write a fragment data to the store.
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param fragment data to be written to the store
   * @param cacheKey [CacheKey] to be used as root record key
   * @param fragmentData [Fragment.Data] to be written to the store
   * @return the changed keys
   *
   * @see publish
   */
  fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): Set<String>

  /**
   * Write operation data to the optimistic store.
   * This is a synchronous operation that might block if the underlying cache is doing IO.
   *
   * @param operation     [Operation] response data of which should be written to the store
   * @param operationData [Operation.Data] operation response data to be written to the store
   * @param mutationId    mutation unique identifier
   * @return the changed keys
   *
   * @see publish
   */
  fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
  ): Set<String>

  /**
   * Rollback operation data optimistic updates.
   * This is a synchronous operation that might block if the underlying cache is doing IO.
   *
   * @param mutationId mutation unique identifier
   * @return the changed keys
   */
  fun rollbackOptimisticUpdates(
      mutationId: Uuid,
  ): Set<String>

  /**
   * Clear all records from this [ApolloStore].
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @return `true` if all records were successfully removed, `false` otherwise
   */
  fun clearAll(): Boolean

  /**
   * Remove cache record by the key
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param cacheKey of record to be removed
   * @param cascade defines if remove operation is propagated to the referenced entities
   * @return `true` if the record was successfully removed, `false` otherwise
   */
  fun remove(cacheKey: CacheKey, cascade: Boolean = true): Boolean

  /**
   * Remove a list of cache records
   * This is an optimized version of [remove] for caches that can batch operations
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param cacheKeys keys of records to be removed
   * @return the number of records that have been removed
   */
  fun remove(cacheKeys: List<CacheKey>, cascade: Boolean = true): Int

  /**
   * Normalize [data] to a map of [Record] keyed by [Record.key].
   */
  fun <D : Operation.Data> normalize(
      operation: Operation<D>,
      data: D,
      customScalarAdapters: CustomScalarAdapters,
  ): Map<String, Record>

  /**
   * @param keys A set of keys of [Record] which have changed.
   */
  suspend fun publish(keys: Set<String>)

  /**
   * Direct access to the cache.
   * This is a synchronous operation that might block if the underlying cache is doing IO.
   *
   * @param block a function that can access the cache.
   */
  fun <R> accessCache(block: (NormalizedCache) -> R): R

  /**
   * Dump the content of the store for debugging purposes.
   * This is a synchronous operation that might block if the underlying cache is doing IO.
   */
  fun dump(): Map<KClass<*>, Map<String, Record>>

  /**
   * Release resources associated with this store.
   */
  fun dispose()
}

fun ApolloStore(
    normalizedCacheFactory: NormalizedCacheFactory,
    cacheKeyGenerator: CacheKeyGenerator = TypePolicyCacheKeyGenerator,
    cacheResolver: CacheResolver = FieldPolicyCacheResolver,
): ApolloStore = DefaultApolloStore(
    normalizedCacheFactory = normalizedCacheFactory,
    cacheKeyGenerator = cacheKeyGenerator,
    metadataGenerator = EmptyMetadataGenerator,
    cacheResolver = cacheResolver,
    recordMerger = DefaultRecordMerger,
    fieldKeyGenerator = DefaultFieldKeyGenerator,
    embeddedFieldsProvider = DefaultEmbeddedFieldsProvider,
)

@ApolloExperimental
fun ApolloStore(
    normalizedCacheFactory: NormalizedCacheFactory,
    cacheKeyGenerator: CacheKeyGenerator = TypePolicyCacheKeyGenerator,
    metadataGenerator: MetadataGenerator = EmptyMetadataGenerator,
    apolloResolver: ApolloResolver = FieldPolicyApolloResolver,
    recordMerger: RecordMerger = DefaultRecordMerger,
    fieldKeyGenerator: FieldKeyGenerator = DefaultFieldKeyGenerator,
    embeddedFieldsProvider: EmbeddedFieldsProvider = DefaultEmbeddedFieldsProvider,
): ApolloStore = DefaultApolloStore(
    normalizedCacheFactory = normalizedCacheFactory,
    cacheKeyGenerator = cacheKeyGenerator,
    metadataGenerator = metadataGenerator,
    cacheResolver = apolloResolver,
    recordMerger = recordMerger,
    fieldKeyGenerator = fieldKeyGenerator,
    embeddedFieldsProvider = embeddedFieldsProvider,
)

/**
 * Interface that marks all interceptors added when configuring a `store()` on ApolloClient.Builder.
 */
internal interface ApolloStoreInterceptor : ApolloInterceptor
