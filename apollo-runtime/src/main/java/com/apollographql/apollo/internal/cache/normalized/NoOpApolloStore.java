package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
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
public final class NoOpApolloStore implements ApolloStore, ReadableStore, WriteableStore {

  @Override public Set<String> merge(@Nonnull Collection<Record> recordCollection, @Nonnull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Nullable @Override public Record read(@Nonnull String key, @Nonnull CacheHeaders cacheHeaders) {
    return null;
  }

  @Override public Collection<Record> read(@Nonnull Collection<String> keys, @Nonnull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Override public void subscribe(RecordChangeSubscriber subscriber) {
  }

  @Override public void unsubscribe(RecordChangeSubscriber subscriber) {
  }

  @Override public void publish(Set<String> keys) {
  }

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

  @Override public <R> R readTransaction(Transaction<ReadableStore, R> transaction) {
    return transaction.execute(this);
  }

  @Override public <R> R writeTransaction(Transaction<WriteableStore, R> transaction) {
    return transaction.execute(this);
  }

  @Override public NormalizedCache normalizedCache() {
    return null;
  }

  @Override public CacheKeyResolver cacheKeyResolver() {
    return null;
  }

  @Nullable @Override public <D extends Operation.Data, T, V extends Operation.Variables> T read(
      @Nonnull Operation<D, T, V> operation) {
    return null;
  }

  @Nonnull @Override public <D extends Operation.Data, T, V extends Operation.Variables> Response<T> read(
      @Nonnull Operation<D, T, V> operation, @Nonnull ResponseFieldMapper<D> responseFieldMapper,
      @Nonnull ResponseNormalizer<Record> responseNormalizer, @Nonnull CacheHeaders cacheHeaders) {
    return Response.<T>builder(operation).build();
  }

  @Nullable @Override public <F extends GraphqlFragment> F read(@Nonnull ResponseFieldMapper<F> fieldMapper,
      @Nonnull CacheKey cacheKey, @Nonnull Operation.Variables variables) {
    return null;
  }

  @Override public void write(@Nonnull Operation operation, @Nonnull Operation.Data operationData) {
  }
}
