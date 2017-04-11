package com.apollographql.apollo.cache.normalized.lru;

import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordFieldAdapter;
import com.nytimes.android.external.cache.Cache;
import com.nytimes.android.external.cache.CacheBuilder;
import com.nytimes.android.external.cache.Weigher;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A {@link NormalizedCache} backed by an in memory {@link Cache}. Can be configured with an optional secondaryCache
 * {@link NormalizedCache}, which will be used as a backup if a {@link Record} is not present in the primary cache.
 *
 * A common configuration is to have secondary {@link com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCache}
 * cache.
 */
public final class LruNormalizedCache extends NormalizedCache {

  private final Cache<String, Record> lruCache;
  private final Optional<NormalizedCache> secondaryCache;

  LruNormalizedCache(final RecordFieldAdapter recordFieldAdapter,
      EvictionPolicy evictionPolicy,
      Optional<NormalizedCacheFactory> secondaryNormalizedCache) {
    super(recordFieldAdapter);
    this.secondaryCache = secondaryNormalizedCache.transform(new Function<NormalizedCacheFactory, NormalizedCache>() {
      @Nonnull @Override public NormalizedCache apply(@Nonnull NormalizedCacheFactory normalizedCacheFactory) {
        return normalizedCacheFactory.createNormalizedCache(recordFieldAdapter);
      }
    });
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

  @Nullable public NormalizedCache secondaryCache() {
    return secondaryCache.get();
  }

  @Nullable @Override public Record loadRecord(final String key) {
    if (secondaryCache.isPresent()) {
      try {
        return lruCache.get(key, new Callable<Record>() {
          @Override public Record call() throws Exception {
            Record record = secondaryCache.get().loadRecord(key);
            // get(key, callable) requires non-null. If null, an exception should be
            //thrown, which will be converted to null in the catch clause.
            if (record == null) {
              throw new Exception(String.format("Record{key=%s} not present in secondary cache", key));
            }
            return record;
          }
        });
      } catch (Exception e) {
        return null;
      }
    }
    return lruCache.getIfPresent(key);
  }

  @Nonnull @Override public Set<String> merge(Record apolloRecord) {
    if (secondaryCache.isPresent()) {
      secondaryCache.get().merge(apolloRecord);
    }
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
    clearPrimaryCache();
    clearSecondaryCache();
  }

  /**
   * Clears all records from the in-memory LRU cache. The secondary cache will *not* be cleared.
   *
   * This method is **not** guaranteed to be thread safe. It should be run inside a write transaction in
   * {@link com.apollographql.apollo.cache.normalized.ApolloStore}, obtained from
   * {@link com.apollographql.apollo.ApolloClient#apolloStore()}.
   */
  public void clearPrimaryCache() {
    lruCache.invalidateAll();
  }

  /**
   * Clear all records from the secondary cache. Records in the in-memory LRU cache will remain.
   *
   * This method is **not** guaranteed to be thread safe. It should be run inside a write transaction in
   * {@link com.apollographql.apollo.cache.normalized.ApolloStore}, obtained from
   * {@link com.apollographql.apollo.ApolloClient#apolloStore()}.
   */
  public void clearSecondaryCache() {
    if (secondaryCache.isPresent()) {
      secondaryCache.get().clearAll();
    }
  }

}
