package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.internal.cache.normalized.NoOpApolloStore;
import com.apollographql.apollo.internal.cache.normalized.ReadableCache;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.cache.normalized.Transaction;
import com.apollographql.apollo.internal.cache.normalized.WriteableCache;

import java.util.Map;
import java.util.Set;

/**
 * ApolloStore exposes a thread-safe api to access a {@link com.apollographql.apollo.cache.normalized.NormalizedCache}.
 * It also maintains a list of {@link RecordChangeSubscriber} that will be notified with changed records.
 *
 */
public interface ApolloStore {

  /**
   * Listens to changed record keys dispatched via {@link #publish(Set)}.
   */
  interface RecordChangeSubscriber {

    /**
     *
     * @param changedRecordKeys A set of record keys which correspond to records which have had content changes.
     */
     void onCacheRecordsChanged(Set<String> changedRecordKeys);
  }

  void subscribe(RecordChangeSubscriber subscriber);

  void unsubscribe(RecordChangeSubscriber subscriber);

  /**
   *
   * @param keys A set of keys of {@link Record} which have changed
   */
  void publish(Set<String> keys);

  /**
   * Clear all records from this {@link ApolloStore}
   */
  void clearAll();

  /**
   *
   * @return The {@link ResponseNormalizer} used to generate normalized records
   */
  ResponseNormalizer<Map<String, Object>> networkResponseNormalizer();

  /**
   *
   * @return
   */
  ResponseNormalizer<Record> cacheResponseNormalizer();

  /**
   *
   * @param transaction
   * @param <R> The result type of this read operation
   * @return A result from
   */
  <R> R readTransaction(Transaction<ReadableCache, R> transaction);

  <R> R writeTransaction(Transaction<WriteableCache, R> transaction);

  ApolloStore NO_APOLLO_STORE = new NoOpApolloStore();
}
