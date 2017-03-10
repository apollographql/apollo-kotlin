package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;

import static com.apollographql.android.impl.util.Utils.checkNotNull;

public final class RealCache implements Cache {
  private final CacheStore cacheStore;
  private final CacheKeyResolver cacheKeyResolver;
  private final ReadWriteLock lock;

  public RealCache(@Nonnull CacheStore cacheStore, @Nonnull CacheKeyResolver cacheKeyResolver) {
    this.cacheStore = cacheStore;
    this.cacheKeyResolver = cacheKeyResolver;
    this.lock = new ReentrantReadWriteLock();
  }

  @Override @Nonnull public ResponseNormalizer responseNormalizer() {
    return new ResponseNormalizer(cacheKeyResolver);
  }

  @Override public void write(@Nonnull Record record) {
    lock.writeLock().lock();
    try {
      cacheStore.merge(checkNotNull(record, "record == null"));
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public void write(@Nonnull Collection<Record> recordSet) {
    lock.writeLock().lock();
    try {
      cacheStore.merge(checkNotNull(recordSet, "recordSet == null"));
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override public Record read(@Nonnull String key) {
    lock.readLock().lock();
    try {
      return cacheStore.loadRecord(checkNotNull(key, "key == null"));
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override public Collection<Record> read(@Nonnull Collection<String> keys) {
    lock.readLock().lock();
    try {
      return cacheStore.loadRecords(checkNotNull(keys, "keys == null"));
    } finally {
      lock.readLock().unlock();
    }
  }
}
