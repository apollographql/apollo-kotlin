package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An alternative to {@link RealApolloStore} for when a no-operation cache
 * is needed.
 */
public final class NoOpApolloStore implements ApolloStore, ReadableCache, WriteableCache {

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

  @Override public void clearAll() {

  }

  @Override public ResponseNormalizer<Map<String, Object>> networkResponseNormalizer() {
    //noinspection unchecked
    return ResponseNormalizer.NO_OP_NORMALIZER;
  }

  @Override public ResponseNormalizer<Record> cacheResponseNormalizer() {
    //noinspection unchecked
    return ResponseNormalizer.NO_OP_NORMALIZER;
  }

  @Override public <R> R readTransaction(Transaction<ReadableCache, R> transaction) {
    return transaction.execute(this);
  }

  @Override public <R> R writeTransaction(Transaction<WriteableCache, R> transaction) {
    return transaction.execute(this);
  }

  @Override public NormalizedCache normalizedCache() {
    return null;
  }
}
