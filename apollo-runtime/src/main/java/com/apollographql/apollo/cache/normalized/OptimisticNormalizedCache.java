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
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public final class OptimisticNormalizedCache extends NormalizedCache {
  private final Cache<String, Record> lruCache = CacheBuilder.newBuilder().build();

  @Nullable @Override public Record loadRecord(@Nonnull final String key, @Nonnull final CacheHeaders cacheHeaders) {
    checkNotNull(key, "key == null");
    checkNotNull(cacheHeaders, "cacheHeaders == null");

    try {
      return lruCache.get(key, new Callable<Record>() {
        @Override public Record call() throws Exception {
          return nextCache().flatMap(new Function<NormalizedCache, Optional<Record>>() {
            @Nonnull @Override public Optional<Record> apply(@Nonnull NormalizedCache cache) {
              return Optional.fromNullable(cache.loadRecord(key, cacheHeaders));
            }
          }).get(); // lruCache.get(key, callable) requires non-null.
        }
      });
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

    final Record oldRecord = lruCache.getIfPresent(record.key());
    if (oldRecord == null) {
      lruCache.put(record.key(), record);
      return Collections.emptySet();
    } else {
      Set<String> changedKeys = oldRecord.mergeWith(record);
      //re-insert to trigger new weight calculation
      lruCache.put(record.key(), oldRecord);
      return changedKeys;
    }
  }

  @Nonnull public Set<String> removeOptimisticUpdates(@Nonnull final UUID version) {
    checkNotNull(version, "version == null");

    Map<String, Record> cachedRecords = lruCache.asMap();
    List<String> invalidateKeys = new ArrayList<>();
    for (Map.Entry<String, Record> cachedRecordEntry : cachedRecords.entrySet()) {
      if (version.equals(cachedRecordEntry.getValue().version()) || cachedRecordEntry.getValue().version() == null) {
        invalidateKeys.add(cachedRecordEntry.getKey());
      }
    }
    lruCache.invalidateAll(invalidateKeys);

    return new HashSet<>(invalidateKeys);
  }
}
