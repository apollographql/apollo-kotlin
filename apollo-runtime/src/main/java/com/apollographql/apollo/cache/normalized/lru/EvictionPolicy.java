package com.apollographql.apollo.cache.normalized.lru;

import com.apollographql.android.api.graphql.internal.Optional;

import java.util.concurrent.TimeUnit;

public final class EvictionPolicy {

  public static final EvictionPolicy NO_EVICTION = EvictionPolicy.builder().build();

  private final Optional<Long> maxSizeBytes;
  private final Optional<Long> maxEntries;
  private final Optional<Long> expireAfterAccess;
  private final Optional<TimeUnit> expireAfterAccessTimeUnit;
  private final Optional<Long> expireAfterWrite;
  private final Optional<TimeUnit> expireAfterWriteTimeUnit;

  Optional<Long> maxSizeBytes() {
    return maxSizeBytes;
  }

  Optional<Long> maxEntries() {
    return maxEntries;
  }

  Optional<Long> expireAfterAccess() {
    return expireAfterAccess;
  }

  Optional<TimeUnit> expireAfterAccessTimeUnit() {
    return expireAfterAccessTimeUnit;
  }

  Optional<Long> expireAfterWrite() {
    return expireAfterWrite;
  }

  Optional<TimeUnit> expireAfterWriteTimeUnit() {
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
