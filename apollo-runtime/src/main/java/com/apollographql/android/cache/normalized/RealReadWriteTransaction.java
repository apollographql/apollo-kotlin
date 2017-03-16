package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Set;

import javax.annotation.Nonnull;

final class RealReadWriteTransaction extends RealReadTransaction implements ReadWriteTransaction {

  RealReadWriteTransaction(RealCache cache) {
    super(cache);
  }

  @Nonnull @Override public Set<String> merge(Collection<Record> recordCollection) {
    if (closed) {
      throw new IllegalStateException("Cannot call merge on a transaction which has already closed.");
    }
    return this.cache.merge(recordCollection);
  }

  @Override public void close() {
    this.closed = true;
    cache.closeWrite();
  }
}
