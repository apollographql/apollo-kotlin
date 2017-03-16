package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

public interface Cache {

  interface RecordChangeSubscriber {
     void onCacheKeysChanged(Set<String> changedCacheKeys);
  }

  void subscribe(RecordChangeSubscriber subscriber);

  void unsubscribe(RecordChangeSubscriber subscriber);

  void publish(Set<String> keys);

  ReadTransaction readTransaction();

  ReadWriteTransaction writeTransaction();

  ResponseNormalizer responseNormalizer();

  Cache NO_CACHE = new Cache() {

    @Override public void subscribe(RecordChangeSubscriber subscriber) { }

    @Override public void unsubscribe(RecordChangeSubscriber subscriber) { }

    @Override public void publish(Set<String> keys) { }

    @Override public ReadTransaction readTransaction() {
        return new ReadTransaction() {
          @Override public Record read(@Nonnull String key) {
            return null;
          }

          @Override public Collection<Record> read(@Nonnull Collection<String> keys) {
            return Collections.emptySet();
          }

          @Override public void close() { }
        };
    }

    @Override public ReadWriteTransaction writeTransaction() {
      return null;
    }

    @Override public ResponseNormalizer responseNormalizer() {
      return ResponseNormalizer.NO_OP_NORMALIZER;
    }

  };
}
