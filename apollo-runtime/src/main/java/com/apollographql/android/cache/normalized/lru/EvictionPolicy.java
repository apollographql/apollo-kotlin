package com.apollographql.android.cache.normalized.lru;

import android.app.ActivityManager;
import android.content.Context;

import com.apollographql.android.api.graphql.internal.Optional;

import java.util.concurrent.TimeUnit;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP;

public final class EvictionPolicy {

  public static final EvictionPolicy NO_EVICTION = EvictionPolicy.builder().build();

  private final Optional<Long> maxSizeBytes;
  private final Optional<Long> maxEntries;
  private final Optional<Long> expireAfterAccess;
  private final Optional<TimeUnit> expireAfterAccessTimeUnit;
  private final Optional<Long> expireAfterWrite;
  private final Optional<TimeUnit> expireAfterWriteTimeUnit;

  public Optional<Long> maxSizeBytes() {
    return maxSizeBytes;
  }

  public Optional<Long> maxEntries() {
    return maxEntries;
  }

  public Optional<Long> expireAfterAccess() {
    return expireAfterAccess;
  }

  public Optional<TimeUnit> expireAfterAccessTimeUnit() {
    return expireAfterAccessTimeUnit;
  }

  public Optional<Long> expireAfterWrite() {
    return expireAfterWrite;
  }

  public Optional<TimeUnit> expireAfterWriteTimeUnit() {
    return expireAfterWriteTimeUnit;
  }

  public static EvictionPolicy.Builder builder() {
    return new EvictionPolicy.Builder();
  }

  public static class Builder {

    private Builder() { }

    private Optional<Long> maxSizeBytes = Optional.absent();
    private Optional<Long> maxEntries = Optional.absent();
    private Optional<Long> expireAfterAccess = Optional.absent();
    private Optional<TimeUnit> expireAfterAccessTimeUnit = Optional.absent();
    private Optional<Long> expireAfterWrite = Optional.absent();
    private Optional<TimeUnit> expireAfterWriteTimeUnit = Optional.absent();

    public EvictionPolicy.Builder deviceBasedMaxSize(Context context) {
      this.maxSizeBytes = Optional.of(calculateDeviceMemoryCacheSize(context));
      return this;
    }

    public EvictionPolicy.Builder maxSizeBytes(long maxSizeBytes) {
      this.maxSizeBytes = Optional.of(maxSizeBytes);
      return this;
    }

    public EvictionPolicy.Builder maxEntries(long maxEntries) {
      this.maxEntries = Optional.of(maxEntries);
      return this;
    }

    public EvictionPolicy.Builder expireAfterAccess(long time, TimeUnit timeUnit) {
      this.expireAfterAccess = Optional.of(time);
      this.expireAfterAccessTimeUnit = Optional.of(timeUnit);
      return this;
    }

    public EvictionPolicy.Builder expireAfterWrite(long time, TimeUnit timeUnit) {
      this.expireAfterWrite = Optional.of(time);
      this.expireAfterWriteTimeUnit = Optional.of(timeUnit);
      return this;
    }

    public EvictionPolicy build() {
      return new EvictionPolicy(maxSizeBytes, maxEntries, expireAfterAccess, expireAfterAccessTimeUnit,
          expireAfterWrite, expireAfterWriteTimeUnit);
    }

  }

  /**
   * Derived from https://github.com/square/picasso cache sizing.
   */
  private static long calculateDeviceMemoryCacheSize(Context context) {
    ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
    boolean largeHeap = (context.getApplicationInfo().flags & FLAG_LARGE_HEAP) != 0;
    int memoryClass = largeHeap ? activityManager.getLargeMemoryClass() : activityManager.getMemoryClass();
    // Target ~15% of the available heap.
    return (int) (1024L * 1024L * memoryClass / 7);
  }

  private EvictionPolicy(Optional<Long> maxSizeBytes, Optional<Long> maxEntries, Optional<Long> expireAfterAccess,
      Optional<TimeUnit> expireAfterAccessTimeUnit, Optional<Long> expireAfterWrite, Optional<TimeUnit>
      expireAfterWriteTimeUnit) {
    this.maxSizeBytes = maxSizeBytes;
    this.maxEntries = maxEntries;
    this.expireAfterAccess = expireAfterAccess;
    this.expireAfterAccessTimeUnit = expireAfterAccessTimeUnit;
    this.expireAfterWrite = expireAfterWrite;
    this.expireAfterWriteTimeUnit = expireAfterWriteTimeUnit;
  }

}
