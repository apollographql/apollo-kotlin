package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.cache.CacheHeaders;
import com.nytimes.android.external.cache.Cache;
import com.nytimes.android.external.cache.CacheBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class OptimisticNormalizedCache extends NormalizedCache {
  private final Cache<String, RecordJournal> lruCache = CacheBuilder.newBuilder().build();

  @Nullable @Override public Record loadRecord(@NotNull final String key, @NotNull final CacheHeaders cacheHeaders) {
    checkNotNull(key, "key == null");
    checkNotNull(cacheHeaders, "cacheHeaders == null");

    try {
      @Nullable final Record nonOptimisticRecord;
      if (getNextCache() != null) {
        nonOptimisticRecord = getNextCache().loadRecord(key, cacheHeaders);
      } else {
        nonOptimisticRecord = null;
      }
      final RecordJournal journal = lruCache.getIfPresent(key);
      if (journal != null) {
        if (nonOptimisticRecord != null) {
          final Record result = nonOptimisticRecord.clone();
          result.mergeWith(journal.snapshot);
          return result;
        } else {
          return journal.snapshot.clone();
        }
      } else {
        return nonOptimisticRecord;
      }
    } catch (Exception ignore) {
      return null;
    }
  }

  @Override public void clearAll() {
    lruCache.invalidateAll();
    if (getNextCache() != null) {
      getNextCache().clearAll();
    }
  }

  @Override public boolean remove(@NotNull final CacheKey cacheKey, final boolean cascade) {
    checkNotNull(cacheKey, "cacheKey == null");

    boolean result;
    if (getNextCache() != null) {
      result = getNextCache().remove(cacheKey, cascade);
    } else {
      result = false;
    }

    RecordJournal recordJournal = lruCache.getIfPresent(cacheKey.key());
    if (recordJournal != null) {
      lruCache.invalidate(cacheKey.key());
      result = true;

      if (cascade) {
        for (CacheReference cacheReference : recordJournal.snapshot.referencedFields()) {
          result = result & remove(new CacheKey(cacheReference.key()), true);
        }
      }
    }

    return result;
  }

  @NotNull public Set<String> mergeOptimisticUpdates(@NotNull Collection<Record> recordSet) {
    Set<String> aggregatedDependentKeys = new LinkedHashSet<>();
    for (Record record : recordSet) {
      aggregatedDependentKeys.addAll(mergeOptimisticUpdate(record));
    }
    return aggregatedDependentKeys;
  }

  @NotNull public Set<String> mergeOptimisticUpdate(@NotNull final Record record) {
    checkNotNull(record, "record == null");

    final RecordJournal journal = lruCache.getIfPresent(record.key());
    if (journal == null) {
      lruCache.put(record.key(), new RecordJournal(record));
      return Collections.singleton(record.key());
    } else {
      return journal.commit(record);
    }
  }

  @NotNull public Set<String> removeOptimisticUpdates(@NotNull final UUID mutationId) {
    checkNotNull(mutationId, "mutationId == null");

    Set<String> changedCacheKeys = new HashSet<>();
    Set<String> removedKeys = new HashSet<>();
    Map<String, RecordJournal> recordJournals = lruCache.asMap();
    for (Map.Entry<String, RecordJournal> entry : recordJournals.entrySet()) {
      String cacheKey = entry.getKey();
      RecordJournal journal = entry.getValue();
      changedCacheKeys.addAll(journal.revert(mutationId));
      if (journal.history.isEmpty()) {
        removedKeys.add(cacheKey);
      }
    }
    lruCache.invalidateAll(removedKeys);
    return changedCacheKeys;
  }

  @NotNull @Override
  protected Set<String> performMerge(@NotNull Record apolloRecord, @NotNull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Override public Map<Class<?>, Map<String, Record>> dump() {
    Map<String, Record> records = new LinkedHashMap<>();
    for (Map.Entry<String, RecordJournal> entry : lruCache.asMap().entrySet()) {
      records.put(entry.getKey(), entry.getValue().snapshot);
    }

    Map<Class<?>, Map<String, Record>> dump = new LinkedHashMap<>();
    dump.put(this.getClass(), Collections.unmodifiableMap(records));
    if (getNextCache() != null) {
      dump.putAll(getNextCache().dump());
    }
    return dump;
  }

  private static final class RecordJournal {
    Record snapshot;
    final List<Record> history = new ArrayList<>();

    RecordJournal(Record mutationRecord) {
      this.snapshot = mutationRecord.clone();
      this.history.add(mutationRecord.clone());
    }

    /**
     * Commits new version of record to the history and invalidate snapshot version.
     */
    Set<String> commit(Record record) {
      history.add(history.size(), record.clone());
      return snapshot.mergeWith(record);
    }

    /**
     * Lookups record by mutation id, if it's found removes it from the history and invalidates snapshot record. Snapshot record is
     * superposition of all record versions in the history.
     */
    Set<String> revert(UUID mutationId) {
      int recordIndex = -1;
      for (int i = 0; i < history.size(); i++) {
        if (mutationId.equals(history.get(i).mutationId())) {
          recordIndex = i;
          break;
        }
      }

      if (recordIndex == -1) {
        return Collections.emptySet();
      }

      Set<String> changedKeys = new HashSet<>();
      changedKeys.add(history.remove(recordIndex).key());
      for (int i = Math.max(0, recordIndex - 1); i < history.size(); i++) {
        Record record = history.get(i);
        if (i == Math.max(0, recordIndex - 1)) {
          snapshot = record.clone();
        } else {
          changedKeys.addAll(snapshot.mergeWith(record));
        }
      }
      return changedKeys;
    }
  }
}
