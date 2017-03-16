package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

public interface Cache {

  interface RecordChangeSubscriber {
     void onDependentKeysChanged();
  }

  void subscribe(RecordChangeSubscriber subscriber, Set<String> dependentKeys);

  void unsubscribe(RecordChangeSubscriber subscriber);

  ReadTransaction readTransaction();

  WriteTransaction writeTransaction();

  ResponseNormalizer responseNormalizer();

  Cache NO_CACHE = new Cache() {

    @Override public void subscribe(RecordChangeSubscriber subscriber, Set<String> dependentKeys) { }

    @Override public void unsubscribe(RecordChangeSubscriber subscriber) { }

    @Override public ReadTransaction readTransaction() {
      return new ReadTransaction() {
        @Override public Record read(@Nonnull String key) { return null; }

        @Override public Collection<Record> read(@Nonnull Collection<String> keys) { return null; }

        @Override public void finishRead() { }
      };
    }

    @Override public WriteTransaction writeTransaction() {
      return new WriteTransaction() {
        @Override public void writeAndFinish(@Nonnull Collection<Record> recordSet) { }
      };
    }

    @Override public ResponseNormalizer responseNormalizer() {
      return ResponseNormalizer.NO_OP_NORMALIZER;
    }

  };
}
