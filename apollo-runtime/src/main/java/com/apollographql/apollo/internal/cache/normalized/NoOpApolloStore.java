package com.apollographql.apollo.internal.cache.normalized;

import com.apollographql.apollo.api.GraphqlFragment;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An alternative to {@link RealApolloStore} for when a no-operation cache is needed.
 */
public final class NoOpApolloStore implements ApolloStore, ReadableStore, WriteableStore {

  @Override public Set<String> merge(@Nonnull Collection<Record> recordCollection, @Nonnull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Override public Set<String> merge(Record record, @Nonnull CacheHeaders cacheHeaders) {
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

  @Nonnull @Override public ApolloStoreOperation<Boolean> clearAll() {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override public ApolloStoreOperation<Boolean> remove(@Nonnull CacheKey cacheKey) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override public ApolloStoreOperation<Integer> remove(@Nonnull List<CacheKey> cacheKeys) {
    return ApolloStoreOperation.emptyOperation(0);
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

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<T> read(
      @Nonnull Operation<D, T, V> operation) {
    return ApolloStoreOperation.emptyOperation(null);
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Response<T>> read(
      @Nonnull Operation<D, T, V> operation, @Nonnull ResponseFieldMapper<D> responseFieldMapper,
      @Nonnull ResponseNormalizer<Record> responseNormalizer, @Nonnull CacheHeaders cacheHeaders) {
    return ApolloStoreOperation.emptyOperation(Response.<T>builder(operation).build());
  }

  @Nonnull @Override
  public <F extends GraphqlFragment> ApolloStoreOperation<F> read(@Nonnull ResponseFieldMapper<F> fieldMapper,
      @Nonnull CacheKey cacheKey, @Nonnull Operation.Variables variables) {
    return ApolloStoreOperation.emptyOperation(null);
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Set<String>> write(
      @Nonnull Operation<D, T, V> operation, @Nonnull D operationData) {
    return ApolloStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Boolean> writeAndPublish(
      @Nonnull Operation<D, T, V> operation, @Nonnull D operationData) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override
  public ApolloStoreOperation<Set<String>> write(@Nonnull GraphqlFragment fragment, @Nonnull CacheKey cacheKey,
      @Nonnull Operation.Variables variables) {
    return ApolloStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @Nonnull @Override
  public ApolloStoreOperation<Boolean> writeAndPublish(@Nonnull GraphqlFragment fragment, @Nonnull CacheKey cacheKey,
      @Nonnull Operation.Variables variables) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Set<String>>
  writeOptimisticUpdates(@Nonnull Operation<D, T, V> operation, @Nonnull D operationData, @Nonnull UUID mutationId) {
    return ApolloStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @Nonnull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Boolean>
  writeOptimisticUpdatesAndPublish(@Nonnull Operation<D, T, V> operation, @Nonnull D operationData,
      @Nonnull UUID mutationId) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override
  public ApolloStoreOperation<Boolean> rollbackOptimisticUpdatesAndPublish(@Nonnull UUID mutationId) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @Nonnull @Override public ApolloStoreOperation<Set<String>> rollbackOptimisticUpdates(@Nonnull UUID mutationId) {
    return ApolloStoreOperation.emptyOperation(Collections.<String>emptySet());
  }
}
