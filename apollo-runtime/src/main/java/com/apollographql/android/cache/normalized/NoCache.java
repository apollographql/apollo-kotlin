package com.apollographql.android.cache.normalized;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An alternative to {@link RealCache} for when a no operation cache
 * is needed.
 */
final class NoCache implements Cache, ReadableCache, WriteableCache {

  @Override public Set<String> merge(Collection<Record> recordCollection) {
    return Collections.emptySet();
  }

  @Nullable @Override public Record read(@Nonnull String key) {
    return null;
  }

  @Override public Collection<Record> read(@Nonnull Collection<String> keys) {
    return Collections.emptySet();
  }

  @Override public void subscribe(RecordChangeSubscriber subscriber) { }

  @Override public void unsubscribe(RecordChangeSubscriber subscriber) { }

  @Override public void publish(Set<String> keys) { }

  @Override public <R> Transaction<ReadableCache, R> readTransaction() {
    return new Transaction<ReadableCache, R>() {
      @Nullable @Override public R execute(Transactional<ReadableCache, R> transactional) {
        return transactional.call(NoCache.this);
      }
    };
  }

  @Override public <R> Transaction<WriteableCache, R> writeTransaction() {
    return new Transaction<WriteableCache, R>() {
      @Nullable @Override public R execute(Transactional<WriteableCache, R> transactional) {
        return transactional.call(NoCache.this);
      }
    };
  }

  @Override public ResponseNormalizer responseNormalizer() {
    return ResponseNormalizer.NO_OP_NORMALIZER;
  }
}
