package com.apollographql.apollo.cache.normalized.lru;

import com.apollographql.apollo.api.internal.Action;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.ApolloCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;
import com.nytimes.android.external.cache.Cache;
import com.nytimes.android.external.cache.CacheBuilder;
import com.nytimes.android.external.cache.Weigher;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * A {@link NormalizedCache} backed by an in memory {@link Cache}. Can be configured with an optional secondaryCache
 * {@link NormalizedCache}, which will be used as a backup if a {@link Record} is not present in the primary cache.
 *
 * A common configuration is to have secondary SQL cache.
 */
public final class LruNormalizedCache extends NormalizedCache {
  private final Cache<String, Record> lruCache;

  LruNormalizedCache(final RecordFieldJsonAdapter recordFieldAdapter, EvictionPolicy evictionPolicy) {
    super(recordFieldAdapter);
    final CacheBuilder<Object, Object> lruCacheBuilder = CacheBuilder.newBuilder();
    if (evictionPolicy.maxSizeBytes().isPresent()) {
      lruCacheBuilder.maximumWeight(evictionPolicy.maxSizeBytes().get())
          .weigher(new Weigher<String, Record>() {
            @Override public int weigh(String key, Record value) {
              return key.getBytes().length + value.sizeEstimateBytes();
            }
          });
    }
    if (evictionPolicy.maxEntries().isPresent()) {
      lruCacheBuilder.maximumSize(evictionPolicy.maxEntries().get());
    }
    if (evictionPolicy.expireAfterAccess().isPresent()) {
      lruCacheBuilder.expireAfterAccess(evictionPolicy.expireAfterAccess().get(),
          evictionPolicy.expireAfterAccessTimeUnit().get());
    }
    if (evictionPolicy.expireAfterWrite().isPresent()) {
      lruCacheBuilder.expireAfterWrite(evictionPolicy.expireAfterWrite().get(),
          evictionPolicy.expireAfterWriteTimeUnit().get());
    }
    lruCache = lruCacheBuilder.build();
  }

  @Nullable @Override public Record loadRecord(@Nonnull final String key, @Nonnull final CacheHeaders cacheHeaders) {
    final Record record;
    try {
      record = lruCache.get(key, new Callable<Record>() {
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

    if (cacheHeaders.hasHeader(ApolloCacheHeaders.EVICT_AFTER_READ)) {
      lruCache.invalidate(key);
    }

    return record;
  }

  @Nonnull @Override
  public Set<String> merge(@Nonnull final Record apolloRecord, @Nonnull final CacheHeaders cacheHeaders) {
    if (cacheHeaders.hasHeader(ApolloCacheHeaders.DO_NOT_STORE)) {
      return Collections.emptySet();
    }

    //noinspection ResultOfMethodCallIgnored
    nextCache().apply(new Action<NormalizedCache>() {
      @Override public void apply(@Nonnull NormalizedCache cache) {
        cache.merge(apolloRecord, cacheHeaders);
      }
    });

    final Record oldRecord = lruCache.getIfPresent(apolloRecord.key());
    if (oldRecord == null) {
      lruCache.put(apolloRecord.key(), apolloRecord);
      return Collections.emptySet();
    } else {
      Set<String> changedKeys = oldRecord.mergeWith(apolloRecord);

      //re-insert to trigger new weight calculation
      lruCache.put(apolloRecord.key(), oldRecord);
      return changedKeys;
    }
  }

  @Override public void clearAll() {
    //noinspection ResultOfMethodCallIgnored
    nextCache().apply(new Action<NormalizedCache>() {
      @Override public void apply(@Nonnull NormalizedCache cache) {
        cache.clearAll();
      }
    });
    clearCurrentCache();
  }

  @Override public boolean remove(@Nonnull final CacheKey cacheKey) {
    checkNotNull(cacheKey, "cacheKey == null");
    boolean result;

    result = nextCache().map(new Function<NormalizedCache, Boolean>() {
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

  void clearCurrentCache() {
    lruCache.invalidateAll();
  }
}
