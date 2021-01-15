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
   *
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with `true` if all records was
   * successfully removed, `false` otherwise
   */
  fun clearAll(): ApolloStoreOperation<Boolean>

  /**
   * Remove cache record by the key
   *
   * @param cacheKey of record to be removed
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with `true` if record with such key
   * was successfully removed, `false` otherwise
   */
  fun remove(cacheKey: CacheKey): ApolloStoreOperation<Boolean>

  /**
   * Remove cache record by the key
   *
   * @param cacheKey of record to be removed
   * @param cascade defines if remove operation is propagated to the referenced entities
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with `true` if record with such key
   * was successfully removed, `false` otherwise
   */
  fun remove(cacheKey: CacheKey, cascade: Boolean): ApolloStoreOperation<Boolean>

  /**
   * Remove cache records by the list of keys
   *
   * @param cacheKeys keys of records to be removed
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with the count of records been removed
   */
  fun remove(cacheKeys: List<CacheKey>): ApolloStoreOperation<Int>

  /**
   * Run a operation inside a read-lock. Blocks until read-lock is acquired.
   *
   * @param transaction A code block to run once the read lock is acquired.
   * @param <R>         The result type of this read operation.
   * @return A result from the read operation.
  </R> */
  fun <R> readTransaction(transaction: Transaction<ReadableStore, R>): R

  /**
   * Run a operation inside a write-lock. Blocks until write-lock is acquired.
   *
   * @param transaction A code block to run once the write lock is acquired.
   * @param <R>         The result type of this write operation.
   * @return A result from the write operation.
  </R> */
  fun <R> writeTransaction(transaction: Transaction<WriteableStore, R>): R

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
   *
   * @param operation to be read
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with cached data for specified operation
   */
  fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE
  ): ApolloStoreOperation<D>

  /**
   * Read a GraphQL fragment from the store.
   *
   * @param cacheKey    [CacheKey] to be used to find cache record for the fragment
   * @param <F>         type of fragment to be read
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with cached fragment data
  </F> */
  fun <D: Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE
  ): ApolloStoreOperation<D>


  /**
   * Write operation to the store and publish changes of [Record] which have changed, that will notify any [com.apollographql.apollo.ApolloQueryWatcher] that depends on these [Record] to re-fetch.
   *
   * @param operation     [Operation] response data of which should be written to the store
   * @param operationData [Operation.Data] operation response data to be written to the store
   * @param publish       whether or not to publish the changed keys to listeners
   * @return {@ApolloStoreOperation} to be performed returning the changed keys
  </V></T></D> */
  fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      publish: Boolean = true
  ): ApolloStoreOperation<Set<String>>

  /**
   * Write fragment to the store and publish changes of [Record] which have changed, that will notify any ApolloQueryWatcher that
   * depends on these [Record] to re-fetch.
   *
   * @param fragment data to be written to the store
   * @param cacheKey [CacheKey] to be used as root record key
   * @param fragmentData [Fragment.Data] to be written to the store
   * @param publish whether or not to publish the changed keys to listeners
   * @return [ApolloStoreOperation] to be performed the changed keys
   */
  fun <D: Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      publish: Boolean = true
  ): ApolloStoreOperation<Set<String>>

  /**
   * Write operation data to the optimistic store.
   *
   * @param operation     [Operation] response data of which should be written to the store
   * @param operationData [Operation.Data] operation response data to be written to the store
   * @param mutationId    mutation unique identifier
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with set of keys of [Record] which
   * have changed
   */
  fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: UUID
  ): ApolloStoreOperation<Set<String>>

  /**
   * Write operation data to the optimistic store and publish changes of [Record]s which have changed, that will
   * notify any [com.apollographql.apollo.ApolloQueryWatcher] that depends on these [Record] to re-fetch.
   *
   * @param operation     [Operation] response data of which should be written to the store
   * @param operationData [Operation.Data] operation response data to be written to the store
   * @param mutationId    mutation unique identifier
   * @return {@ApolloStoreOperation} to be performed
   */
  fun <D : Operation.Data> writeOptimisticUpdatesAndPublish(
      operation: Operation<D>,
      operationData: D,
      mutationId: UUID
  ): ApolloStoreOperation<Boolean>

  /**
   * Rollback operation data optimistic updates.
   *
   * @param mutationId mutation unique identifier
   * @return {@ApolloStoreOperation} to be performed
   */
  fun rollbackOptimisticUpdates(mutationId: UUID): ApolloStoreOperation<Set<String>>

  /**
   * Rollback operation data optimistic updates and publish changes of [Record]s which have changed, that will
   * notify any [com.apollographql.apollo.ApolloQueryWatcher] that depends on these [Record] to re-fetch.
   *
   * @param mutationId mutation unique identifier
   * @return {@ApolloStoreOperation} to be performed, that will be resolved with set of keys of [Record] which
   * have changed
   */
  fun rollbackOptimisticUpdatesAndPublish(mutationId: UUID): ApolloStoreOperation<Boolean>

  companion object {
    @JvmField
    val NO_APOLLO_STORE: ApolloStore = NoOpApolloStore()
  }
}
