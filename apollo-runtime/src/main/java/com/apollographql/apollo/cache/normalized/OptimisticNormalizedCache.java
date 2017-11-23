package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.api.internal.Action;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.nytimes.android.external.cache.Cache;
import com.nytimes.android.external.cache.CacheBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class OptimisticNormalizedCache extends NormalizedCache {
  private final Cache<String, RecordJournal> lruCache = CacheBuilder.newBuilder().build();

  @Nullable @Override public Record loadRecord(@Nonnull final String key, @Nonnull final CacheHeaders cacheHeaders) {
    checkNotNull(key, "key == null");
    checkNotNull(cacheHeaders, "cacheHeaders == null");

    try {
      final Optional<Record> nonOptimisticRecord = nextCache()
          .flatMap(new Function<NormalizedCache, Optional<Record>>() {
            @Nonnull @Override public Optional<Record> apply(@Nonnull NormalizedCache cache) {
              return Optional.fromNullable(cache.loadRecord(key, cacheHeaders));
            }
          });
      final RecordJournal journal = lruCache.getIfPresent(key);
      if (journal != null) {
        return nonOptimisticRecord.map(new Function<Record, Record>() {
          @Nonnull @Override public Record apply(@Nonnull Record record) {
            Record result = record.clone();
            result.mergeWith(journal.snapshot);
            return result;
          }
        }).or(journal.snapshot.clone());
      } else {
        return nonOptimisticRecord.orNull();
      }
    } catch (Exception ignore) {
      return null;
    }
  }

  @Nonnull @Override public Set<String> merge(@Nonnull final Record record, @Nonnull final CacheHeaders cacheHeaders) {
    checkNotNull(record, "record == null");
    checkNotNull(cacheHeaders, "cacheHeaders == null");

    return nextCache().map(new Function<NormalizedCache, Set<String>>() {
      @Nonnull @Override public Set<String> apply(@Nonnull NormalizedCache cache) {
        return cache.merge(record, cacheHeaders);
      }
    }).or(Collections.<String>emptySet());
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override public void clearAll() {
    lruCache.invalidateAll();
    //noinspection ResultOfMethodCallIgnored
    nextCache().apply(new Action<NormalizedCache>() {
      @Override public void apply(@Nonnull NormalizedCache cache) {
        cache.clearAll();
      }
    });
  }

  @Override public boolean remove(@Nonnull final CacheKey cacheKey) {
    checkNotNull(cacheKey, "cacheKey == null");

    boolean result = nextCache().map(new Function<NormalizedCache, Boolean>() {
      @Nonnull @Override public Boolean apply(@Nonnull NormalizedCache cache) {
        return cache.remove(cacheKey);
      }
    }).or(Boolean.FALSE);

    if (lruCache.getIfPresent(cacheKey.key()) != null) {
      lruCache.invalidate(cacheKey.key());
      result = true;
    }

    return result;
  }

  @Nonnull public Set<String> mergeOptimisticUpdates(@Nonnull Collection<Record> recordSet) {
    Set<String> aggregatedDependentKeys = new LinkedHashSet<>();
    for (Record record : recordSet) {
      aggregatedDependentKeys.addAll(mergeOptimisticUpdate(record));
    }
    return aggregatedDependentKeys;
  }

  @Nonnull public Set<String> mergeOptimisticUpdate(@Nonnull final Record record) {
    checkNotNull(record, "record == null");

    final RecordJournal journal = lruCache.getIfPresent(record.key());
    if (journal == null) {
      lruCache.put(record.key(), new RecordJournal(record));
      return Collections.singleton(record.key());
    } else {
      return journal.commit(record);
    }
  }

  @Nonnull public Set<String> removeOptimisticUpdates(@Nonnull final UUID mutationId) {
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
     * Lookups record by mutation id, if it's found removes it from the history and invalidates snapshot record.
     * Snapshot record is superposition of all record versions in the history.
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
