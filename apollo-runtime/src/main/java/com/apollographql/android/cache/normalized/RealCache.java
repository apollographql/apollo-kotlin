package com.apollographql.android.cache.normalized;


import com.apollographql.android.api.graphql.util.Utils;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnull;

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;

public final class RealCache implements Cache, ReadTransaction, WriteTransaction {
  private final CacheStore cacheStore;
  private final CacheKeyResolver cacheKeyResolver;
  private final ReadWriteLock lock;
  private final Map<RecordChangeSubscriber, Set<String>> subscribers;

  public RealCache(@Nonnull CacheStore cacheStore, @Nonnull CacheKeyResolver cacheKeyResolver) {
    this.cacheStore = cacheStore;
    this.cacheKeyResolver = cacheKeyResolver;
    this.lock = new ReentrantReadWriteLock();
    this.subscribers = new WeakHashMap<>();
  }

  @Override @Nonnull public ResponseNormalizer responseNormalizer() {
    return new ResponseNormalizer(cacheKeyResolver);
  }

  @Override public void subscribe(RecordChangeSubscriber subscriber, Set<String> dependentKeys) {
    subscribers.put(subscriber, dependentKeys);
  }

  @Override public void unsubscribe(RecordChangeSubscriber subscriber) {
    subscribers.remove(subscriber);
  }

  @Override public ReadTransaction readTransaction() {
    lock.readLock().lock();
    return this;
  }

  @Override public Record read(@Nonnull String key) {
    return cacheStore.loadRecord(checkNotNull(key, "key == null"));
  }

  @Override public Collection<Record> read(@Nonnull Collection<String> keys) {
    return cacheStore.loadRecords(checkNotNull(keys, "keys == null"));
  }

  @Override public void finishRead() {
    lock.readLock().unlock();
  }

  @Override public WriteTransaction writeTransaction() {
    lock.writeLock().lock();
    return this;
  }

  @Override public void writeAndFinish(@Nonnull Collection<Record> recordSet) {
    Set<String> changedKeys = Collections.emptySet();
    try {
      changedKeys = cacheStore.merge(checkNotNull(recordSet, "recordSet == null"));
    } finally {
      lock.writeLock().unlock();
    }
    notifyChangedKeySubscribers(changedKeys);
  }

  private void notifyChangedKeySubscribers(Set<String> changedKeys) {
    Map<RecordChangeSubscriber, Set<String>> iterableSubscribers = new LinkedHashMap<>(subscribers);
    for (Map.Entry<RecordChangeSubscriber, Set<String>> subscriberEntry : iterableSubscribers.entrySet()) {
      if (!Utils.areDisjoint(subscriberEntry.getValue(), changedKeys)) {
        subscriberEntry.getKey().onDependentKeysChanged();
      }
    }
  }

}
