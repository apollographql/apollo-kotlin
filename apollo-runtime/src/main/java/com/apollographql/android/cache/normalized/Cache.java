package com.apollographql.android.cache.normalized;

import java.util.Set;

public interface Cache {

  interface RecordChangeSubscriber {
     void onCacheKeysChanged(Set<String> changedCacheKeys);
  }

  void subscribe(RecordChangeSubscriber subscriber);

  void unsubscribe(RecordChangeSubscriber subscriber);

  void publish(Set<String> keys);

  <R> R readTransaction(Transaction<ReadableCache, R> transaction);

  <R> R writeTransaction(Transaction<WriteableCache, R> transaction);

  ResponseNormalizer responseNormalizer();

  Cache NO_CACHE = new NoOpCache();
}
