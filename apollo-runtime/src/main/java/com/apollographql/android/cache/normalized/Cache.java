package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Collections;

import javax.annotation.Nonnull;

public interface Cache {
  ResponseNormalizer responseNormalizer();

  void write(@Nonnull Record record);

  void write(@Nonnull Collection<Record> recordSet);

  Record read(@Nonnull String key);

  Collection<Record> read(@Nonnull Collection<String> keys);

  Cache NO_CACHE = new Cache() {
    @Override public ResponseNormalizer responseNormalizer() {
      return ResponseNormalizer.NO_OP_NORMALIZER;
    }

    @Override public void write(@Nonnull Record record) {
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
