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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An alternative to {@link RealApolloStore} for when a no-operation cache is needed.
 */
public final class NoOpApolloStore implements ApolloStore, ReadableStore, WriteableStore {

  @Override public Set<String> merge(@NotNull Collection<Record> recordCollection, @NotNull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Override public Set<String> merge(Record record, @NotNull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Nullable @Override public Record read(@NotNull String key, @NotNull CacheHeaders cacheHeaders) {
    return null;
  }

  @Override public Collection<Record> read(@NotNull Collection<String> keys, @NotNull CacheHeaders cacheHeaders) {
    return Collections.emptySet();
  }

  @Override public void subscribe(RecordChangeSubscriber subscriber) {
  }

  @Override public void unsubscribe(RecordChangeSubscriber subscriber) {
  }

  @Override public void publish(Set<String> keys) {
  }

  @NotNull @Override public ApolloStoreOperation<Boolean> clearAll() {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @NotNull @Override public ApolloStoreOperation<Boolean> remove(@NotNull CacheKey cacheKey) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @NotNull @Override public ApolloStoreOperation<Integer> remove(@NotNull List<CacheKey> cacheKeys) {
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

  @NotNull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<T> read(
      @NotNull Operation<D, T, V> operation) {
    return ApolloStoreOperation.emptyOperation(null);
  }

  @NotNull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Response<T>> read(
      @NotNull Operation<D, T, V> operation, @NotNull ResponseFieldMapper<D> responseFieldMapper,
      @NotNull ResponseNormalizer<Record> responseNormalizer, @NotNull CacheHeaders cacheHeaders) {
    return ApolloStoreOperation.emptyOperation(Response.<T>builder(operation).build());
  }

  @NotNull @Override
  public <F extends GraphqlFragment> ApolloStoreOperation<F> read(@NotNull ResponseFieldMapper<F> fieldMapper,
      @NotNull CacheKey cacheKey, @NotNull Operation.Variables variables) {
    return ApolloStoreOperation.emptyOperation(null);
  }

  @NotNull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Set<String>> write(
      @NotNull Operation<D, T, V> operation, @NotNull D operationData) {
    return ApolloStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @NotNull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Boolean> writeAndPublish(
      @NotNull Operation<D, T, V> operation, @NotNull D operationData) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @NotNull @Override
  public ApolloStoreOperation<Set<String>> write(@NotNull GraphqlFragment fragment, @NotNull CacheKey cacheKey,
      @NotNull Operation.Variables variables) {
    return ApolloStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @NotNull @Override
  public ApolloStoreOperation<Boolean> writeAndPublish(@NotNull GraphqlFragment fragment, @NotNull CacheKey cacheKey,
      @NotNull Operation.Variables variables) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @NotNull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Set<String>>
  writeOptimisticUpdates(@NotNull Operation<D, T, V> operation, @NotNull D operationData, @NotNull UUID mutationId) {
    return ApolloStoreOperation.emptyOperation(Collections.<String>emptySet());
  }

  @NotNull @Override
  public <D extends Operation.Data, T, V extends Operation.Variables> ApolloStoreOperation<Boolean>
  writeOptimisticUpdatesAndPublish(@NotNull Operation<D, T, V> operation, @NotNull D operationData,
      @NotNull UUID mutationId) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @NotNull @Override
  public ApolloStoreOperation<Boolean> rollbackOptimisticUpdatesAndPublish(@NotNull UUID mutationId) {
    return ApolloStoreOperation.emptyOperation(Boolean.FALSE);
  }

  @NotNull @Override public ApolloStoreOperation<Set<String>> rollbackOptimisticUpdates(@NotNull UUID mutationId) {
    return ApolloStoreOperation.emptyOperation(Collections.<String>emptySet());
  }
}
