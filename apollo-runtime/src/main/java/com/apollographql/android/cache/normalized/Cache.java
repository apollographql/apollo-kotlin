package com.apollographql.android.cache.normalized;

import java.util.Set;

public interface Cache {

  interface RecordChangeSubscriber {
     void onCacheKeysChanged(Set<String> changedCacheKeys);
  }

  void subscribe(RecordChangeSubscriber subscriber);

  void unsubscribe(RecordChangeSubscriber subscriber);

  void publish(Set<String> keys);

  <R> Transaction<ReadableCache, R> readTransaction();

  <R> Transaction<WriteableCache, R> writeTransaction();

  ResponseNormalizer responseNormalizer();

  Cache NO_CACHE = new NoCache();
}
