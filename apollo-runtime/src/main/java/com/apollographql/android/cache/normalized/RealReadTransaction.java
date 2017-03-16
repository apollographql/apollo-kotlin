package com.apollographql.android.cache.normalized;

import java.util.Collection;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

class RealReadTransaction implements ReadTransaction {

  protected RealCache cache;
  protected volatile boolean closed;

  RealReadTransaction(RealCache cache) {
    this.cache = cache;
  }

  @Nullable @Override public Record read(@Nonnull String key) {
    if (closed) {
      throw new IllegalStateException("Cannot call read on a transaction which has already closed.");
    }
    return cache.read(key);
  }

  @Override public Collection<Record> read(@Nonnull Collection<String> keys) {
    if (closed) {
      throw new IllegalStateException("Cannot call read on a transaction which has already closed.");
    }
    return cache.read(keys);
  }

  @Override public void close() {
    cache.closeRead();
    this.closed = true;

  }

}
