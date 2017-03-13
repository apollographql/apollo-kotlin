package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

public interface Cache {

  interface RecordChangeSubscriber {
    /**
     * @return Whether or not to keep the subscription active.
     */
     void onDependentKeysChanged();
  }

  void subscribe(RecordChangeSubscriber subscriber, Set<String> dependentKeys);

  void unsubscribe(RecordChangeSubscriber subscriber);

  ResponseNormalizer responseNormalizer();

  void write(@Nonnull Collection<Record> recordSet);

  Record read(@Nonnull String key);

  Collection<Record> read(@Nonnull Collection<String> keys);

  Cache NO_CACHE = new Cache() {

    @Override public void subscribe(RecordChangeSubscriber subscriber, Set<String> dependentKeys) { }

    @Override public void unsubscribe(RecordChangeSubscriber subscriber) { }

    @Override public ResponseNormalizer responseNormalizer() {
      return ResponseNormalizer.NO_OP_NORMALIZER;
    }

    @Override public void write(@Nonnull Collection<Record> recordSet) {
    }

    @Override public Record read(@Nonnull String key) {
      return null;
    }

    @Override public Collection<Record> read(@Nonnull Collection<String> keys) {
      return Collections.emptyList();
    }
  };
}
