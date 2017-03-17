package com.apollographql.android.cache.normalized;


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;

public final class RealCache implements Cache, ReadableCache, WriteableCache {
  private final CacheStore cacheStore;
  private final CacheKeyResolver cacheKeyResolver;
  private final ReadWriteLock lock;
  private final Set<RecordChangeSubscriber> subscribers;

  public RealCache(@Nonnull CacheStore cacheStore, @Nonnull CacheKeyResolver cacheKeyResolver) {
    this.cacheStore = cacheStore;
    this.cacheKeyResolver = cacheKeyResolver;
    this.lock = new ReentrantReadWriteLock();
    this.subscribers = Collections.newSetFromMap(new WeakHashMap<RecordChangeSubscriber, Boolean>());
  }

  @Override @Nonnull public ResponseNormalizer responseNormalizer() {
    return new ResponseNormalizer(cacheKeyResolver);
  }

  @Override public synchronized void subscribe(RecordChangeSubscriber subscriber) {
    subscribers.add(subscriber);
  }

  @Override public synchronized void unsubscribe(RecordChangeSubscriber subscriber) {
    subscribers.remove(subscriber);
  }

  @Override public void publish(@Nonnull Set<String> changedKeys) {
    checkNotNull(changedKeys, "changedKeys == null");
    if (changedKeys.isEmpty()) {
      return;
    }
    Set<RecordChangeSubscriber> iterableSubscribers;
    synchronized (this) {
      iterableSubscribers = new LinkedHashSet<>(subscribers);
    }
    for (RecordChangeSubscriber subscriber : iterableSubscribers) {
      subscriber.onCacheKeysChanged(changedKeys);
    }
  }

  @Override public <R> R readTransaction(Transaction<ReadableCache, R> transaction) {
    lock.readLock().lock();
    try {
      return transaction.execute(RealCache.this);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override public <R> R writeTransaction(Transaction<WriteableCache, R> transaction) {
    lock.writeLock().lock();
    try {
      return transaction.execute(RealCache.this);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Nullable public Record read(@Nonnull String key) {
    return cacheStore.loadRecord(checkNotNull(key, "key == null"));
  }

  @Nonnull public Collection<Record> read(@Nonnull Collection<String> keys) {
    return cacheStore.loadRecords(checkNotNull(keys, "keys == null"));
  }

  @Nonnull public Set<String> merge(@Nonnull Collection<Record> recordSet) {
    return cacheStore.merge(checkNotNull(recordSet, "recordSet == null"));
  }

}
