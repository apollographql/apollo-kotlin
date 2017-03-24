package com.apollographql.android.cache.normalized.lru;

import android.app.ActivityManager;
import android.content.Context;

import com.apollographql.android.api.graphql.internal.Optional;
import com.apollographql.android.cache.normalized.CacheStore;
import com.apollographql.android.cache.normalized.Record;
import com.nytimes.android.external.cache.Cache;
import com.nytimes.android.external.cache.CacheBuilder;
import com.nytimes.android.external.cache.Weigher;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;

public final class LruCacheStore extends CacheStore {

  private final Cache<String, Record> lruCache;

  public static LruCacheStore create(Context context) {
    return new LruCacheStore(Optional.of(calculateMemoryCacheSize(context)), Optional.<Long>absent());
  }

  public static LruCacheStore createWithMaximumEntrySize(long entrySize) {
    return new LruCacheStore(Optional.<Long>absent(), Optional.of(entrySize));
  }

  public static LruCacheStore createWithMaximumByteSize(long sizeInBytes) {
    return new LruCacheStore(Optional.of(sizeInBytes), Optional.<Long>absent());
  }

  public static LruCacheStore createUnbounded() {
    return new LruCacheStore(Optional.<Long>absent(), Optional.<Long>absent());
  }

  /**
   * Derived from https://github.com/square/picasso cache sizing.
   */
  private static long calculateMemoryCacheSize(Context context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
    boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
    int memoryClass = largeHeap ? activityManager.getLargeMemoryClass() : activityManager.getMemoryClass();
    // Target ~15% of the available heap.
    return (int) (1024L * 1024L * memoryClass / 7);
  }

  private LruCacheStore(Optional<Long> maxSizeBytes, Optional<Long> maxEntries) {
    final CacheBuilder<Object, Object> lruCacheBuilder = CacheBuilder.newBuilder();
    if (maxSizeBytes.isPresent()) {
      lruCacheBuilder.maximumWeight(maxSizeBytes.get())
          .weigher(new Weigher<String, Record>() {
            @Override public int weigh(String key, Record value) {
              return key.getBytes().length + value.sizeEstimateBytes();
            }
          });
    }
    if (maxEntries.isPresent()) {
      lruCacheBuilder.maximumSize(maxEntries.get());
    }
    lruCache = lruCacheBuilder.build();
  }

  @Nullable @Override public Record loadRecord(String key) {
    return lruCache.getIfPresent(key);
  }

  @Nonnull @Override public Set<String> merge(Record apolloRecord) {
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
