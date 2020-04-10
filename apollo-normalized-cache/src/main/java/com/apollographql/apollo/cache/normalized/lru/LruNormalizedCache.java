package com.apollographql.apollo.cache.normalized.lru;

import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.ApolloCacheHeaders;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.nytimes.android.external.cache.Cache;
import com.nytimes.android.external.cache.CacheBuilder;
import com.nytimes.android.external.cache.Weigher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * A {@link NormalizedCache} backed by an in memory {@link Cache}. Can be configured with an optional secondaryCache {@link
 * NormalizedCache}, which will be used as a backup if a {@link Record} is not present in the primary cache.
 * <p>
 * A common configuration is to have secondary SQL cache.
 */
public final class LruNormalizedCache extends NormalizedCache {
  private final Cache<String, Record> lruCache;

  LruNormalizedCache(EvictionPolicy evictionPolicy) {
    final CacheBuilder<Object, Object> lruCacheBuilder = CacheBuilder.newBuilder();
    if (evictionPolicy.maxSizeBytes().isPresent()) {
      lruCacheBuilder.maximumWeight(evictionPolicy.maxSizeBytes().get())
          .weigher((Weigher<String, Record>) (key, value) -> key.getBytes(Charset.defaultCharset()).length + value.sizeEstimateBytes());
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

  @Nullable @Override public Record loadRecord(@NotNull final String key, @NotNull final CacheHeaders cacheHeaders) {
    final Record record;
    try {
      record = lruCache.get(key, new Callable<Record>() {
        @Override public Record call() throws Exception {
          if (getNextCache() != null) {
            return getNextCache().loadRecord(key, cacheHeaders);
          } else {
            return null;
          }
        }
      });
    } catch (Exception ignored) { // Thrown when the nextCache's value is null
      return null;
    }

    if (cacheHeaders.hasHeader(ApolloCacheHeaders.EVICT_AFTER_READ)) {
      lruCache.invalidate(key);
    }

    return record;
  }

  @Override public void clearAll() {
    if (getNextCache() != null) {
      getNextCache().clearAll();
    }
    clearCurrentCache();
  }

  @Override public boolean remove(@NotNull final CacheKey cacheKey, final boolean cascade) {
    checkNotNull(cacheKey, "cacheKey == null");

    boolean result;
    if (getNextCache() != null) {
      result = getNextCache().remove(cacheKey, cascade);
    } else {
      result = false;
    }

    Record record = lruCache.getIfPresent(cacheKey.key());
    if (record != null) {
      lruCache.invalidate(cacheKey.key());
      result = true;

      if (cascade) {
        for (CacheReference cacheReference : record.referencedFields()) {
          result = result & remove(new CacheKey(cacheReference.key()), true);
        }
      }
    }

    return result;
  }

  void clearCurrentCache() {
    lruCache.invalidateAll();
  }

  @NotNull
  @Override protected Set<String> performMerge(@NotNull final Record apolloRecord, @NotNull final CacheHeaders cacheHeaders) {
    final Record oldRecord = lruCache.getIfPresent(apolloRecord.key());
    if (oldRecord == null) {
      lruCache.put(apolloRecord.key(), apolloRecord);
      return apolloRecord.keys();
    } else {
      Set<String> changedKeys = oldRecord.mergeWith(apolloRecord);

      //re-insert to trigger new weight calculation
      lruCache.put(apolloRecord.key(), oldRecord);
      return changedKeys;
    }
  }

  @Override public Map<Class<?>, Map<String, Record>> dump() {
    Map<Class<?>, Map<String, Record>> dump = new LinkedHashMap<>();
    dump.put(this.getClass(), Collections.unmodifiableMap(new LinkedHashMap<>(lruCache.asMap())));
    if (getNextCache() != null) {
      dump.putAll(getNextCache().dump());
    }
    return dump;
  }
}
