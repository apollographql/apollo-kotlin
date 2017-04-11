package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.ApolloClient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A provider of {@link Record} for reading requests from cache.
 *
 * If {@link NormalizedCache#loadRecords(Collection)} returns an empty set while, the request will be considered a
 * cache-miss.
 *
 * To serialize a {@link Record} to a standardized form use {@link #recordAdapter()} which handles
 * call custom scalar types registered on the {@link ApolloClient}.
 *
 * A {@link NormalizedCache} can choose to store records in any manner.
 * See {@link com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCache} for a persistent cache.
 * See {@link com.apollographql.apollo.cache.normalized.lru.LruNormalizedCache} for a in memory cache.
 */
public abstract class NormalizedCache {

  private RecordFieldAdapter recordFieldAdapter;

  /**
   * @param recordFieldAdapter An adapter which can deserialize and deserialize {@link Record}
   */
  public NormalizedCache(RecordFieldAdapter recordFieldAdapter) {
    this.recordFieldAdapter = recordFieldAdapter;
  }

  protected RecordFieldAdapter recordAdapter() {
    return recordFieldAdapter;
  }

  /**
   * @param key The key of the record to read
   * @return The {@link Record} for key. If not present return null.
   */
  @Nullable public abstract Record loadRecord(String key);

  /**
   * Calls through to {@link NormalizedCache#loadRecord(String)}. Implementations should override this method if the
   * underlying storage technology can offer an optimized manner to read multiple records.
   *
   * @param keys The set of {@link Record} keys to read.
   */
  @Nonnull public Collection<Record> loadRecords(Collection<String> keys) {
    List<Record> records = new ArrayList<>(keys.size());
    for (String key : keys) {
      final Record record = loadRecord(key);
      if (record != null) {
        records.add(record);
      }
    }
    return records;
  }

  /**
   * @param record The {@link Record} to merge.
   * @return A set of record field keys that have changed. This set is returned by {@link Record#mergeWith(Record)}.
   */
  @Nonnull public abstract Set<String> merge(Record record);

  /**
   * Calls through to {@link NormalizedCache#merge(Record)}. Implementations should override this method if the
   * underlying storage technology can offer an optimized manner to store multiple records.
   *
   * @param recordSet The set of Records to merge
   * @return A set of record field keys that have changed. This set is returned by {@link Record#mergeWith(Record)}.
   */
  @Nonnull public Set<String> merge(Collection<Record> recordSet) {
    Set<String> aggregatedDependentKeys = new LinkedHashSet<>();
    for (Record record : recordSet) {
      aggregatedDependentKeys.addAll(merge(record));
    }
    return aggregatedDependentKeys;
  }

  /**
   * Clears all records from the cache.
   *
   * Clients should call {@link ApolloClient#clearNormalizedCache()} for a thread-safe access to
   * this method.
   */
  public abstract void clearAll();

}
