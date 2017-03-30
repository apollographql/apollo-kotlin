package com.apollographql.apollo.cache.normalized.lru;

import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.CacheStore;
import com.apollographql.apollo.cache.normalized.Record;
import com.nytimes.android.external.cache.Cache;
import com.nytimes.android.external.cache.CacheBuilder;
import com.nytimes.android.external.cache.Weigher;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class LruCacheStore extends CacheStore {

  private final Cache<String, Record> lruCache;
  private final Optional<CacheStore> secondaryCacheStore;

  public LruCacheStore(EvictionPolicy evictionPolicy) {
    this(evictionPolicy, null);
  }

  public LruCacheStore(EvictionPolicy evictionPolicy, CacheStore secondaryCacheStore) {
    this.secondaryCacheStore = Optional.fromNullable(secondaryCacheStore);
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

  @Nullable @Override public Record loadRecord(final String key) {
    if (secondaryCacheStore.isPresent()) {
      try {
        return lruCache.get(key, new Callable<Record>() {
          @Override public Record call() throws Exception {
            Record record = secondaryCacheStore.get().loadRecord(key);
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
    if (secondaryCacheStore.isPresent()) {
      secondaryCacheStore.get().merge(apolloRecord);
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
}
