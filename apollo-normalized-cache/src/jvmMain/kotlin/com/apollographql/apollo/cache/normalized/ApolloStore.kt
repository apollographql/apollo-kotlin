package com.apollographql.apollo.cache.normalized

import com.apollographql.apollo.api.Fragment
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.ApolloStore.RecordChangeSubscriber
import com.apollographql.apollo.cache.normalized.internal.NoOpApolloStore
import com.apollographql.apollo.cache.normalized.internal.ReadableStore
import com.apollographql.apollo.cache.normalized.internal.Transaction
import com.apollographql.apollo.cache.normalized.internal.WriteableStore
import java.util.UUID

/**
 * ApolloStore exposes a thread-safe api to access a [com.apollographql.apollo.cache.normalized.NormalizedCache].
 * It also maintains a list of [RecordChangeSubscriber] that will be notified with changed records.
 *
 * Most clients should have no need to directly interface with an [ApolloStore].
 */
interface ApolloStore {
  /**
   * Listens to changed record keys dispatched via [.publish].
   */
  interface RecordChangeSubscriber {
    /**
     * @param changedRecordKeys A set of record keys which correspond to records which have had content changes.
     */
    fun onCacheRecordsChanged(changedRecordKeys: Set<String>)
  }

  fun subscribe(subscriber: RecordChangeSubscriber)
  fun unsubscribe(subscriber: RecordChangeSubscriber)

  /**
   * @param keys A set of keys of [Record] which have changed.
   */
  fun publish(keys: Set<String>)

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
   * @return The [NormalizedCache] which backs this ApolloStore.
   */
  fun normalizedCache(): NormalizedCache

  /**
   * @return the [CacheKeyResolver] used for resolving field cache keys
   */
  fun cacheKeyResolver(): CacheKeyResolver

  /**
   * Read GraphQL operation from store.
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param operation to be read
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with cached data for specified operation
   */
  fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE
  ): D?

  /**
   * Read a GraphQL fragment from the store.
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param cacheKey    [CacheKey] to be used to find cache record for the fragment
   * @param <F>         type of fragment to be read
   * @return the fragment's data or null if it's a cache miss
   */
  fun <D: Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): D?


  /**
   * Write operation to the store and optionally publish changes of [Record] which have changed,
   * that will notify any [com.apollographql.apollo.ApolloQueryWatcher] that depends on these [Record] to re-fetch.
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param operation     [Operation] response data of which should be written to the store
   * @param operationData [Operation.Data] operation response data to be written to the store
   * @param publish       whether or not to publish the changed keys to listeners
   * @return the changed keys
   */
  fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
      publish: Boolean = true
  ): Set<String>

  /**
   * Write fragment to the store and optionally publish changes of [Record] which have changed,
   * that will notify any [com.apollographql.apollo.ApolloQueryWatcher] that depends on these [Record] to re-fetch.
   * This is a synchronous operation that might block if the underlying cache is doing IO
   *
   * @param fragment data to be written to the store
   * @param cacheKey [CacheKey] to be used as root record key
   * @param fragmentData [Fragment.Data] to be written to the store
   * @param publish whether or not to publish the changed keys to listeners
   * @return the changed keys
   */
  fun <D: Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
      publish: Boolean = true
  ): Set<String>

  /**
   * Write operation data to the optimistic store.
   *
   * @param operation     [Operation] response data of which should be written to the store
   * @param operationData [Operation.Data] operation response data to be written to the store
   * @param mutationId    mutation unique identifier
   * @return the changed keys
   */
  fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: UUID,
      publish: Boolean = true
  ): Set<String>

  /**
   * Rollback operation data optimistic updates.
   *
   * @param mutationId mutation unique identifier
   * @return the changed keys
   */
  fun rollbackOptimisticUpdates(
      mutationId: UUID,
      publish: Boolean = true
  ): Set<String>

  /**
   * Used internally to get the records. Do not use from outside the apollo libs
   */
  fun <D : Operation.Data> writeOperationWithRecords(operation: Operation<D>, operationData: D, cacheHeaders: CacheHeaders, publish: Boolean): Pair<Set<Record>, Set<String>>

  companion object {
    @JvmField
    val NO_APOLLO_STORE: ApolloStore = NoOpApolloStore()
  }
}
