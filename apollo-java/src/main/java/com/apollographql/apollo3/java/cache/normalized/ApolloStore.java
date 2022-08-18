package com.apollographql.apollo3.java.cache.normalized;

import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Fragment;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders;
import com.apollographql.apollo3.cache.normalized.api.CacheKey;
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator;
import com.apollographql.apollo3.cache.normalized.api.CacheResolver;
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache;
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory;
import com.apollographql.apollo3.cache.normalized.api.Record;
import com.apollographql.apollo3.java.Subscription;
import com.apollographql.apollo3.java.cache.normalized.internal.ApolloStoreAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

public interface ApolloStore {
  @NotNull Subscription observeChangedKeys(@NotNull Function<Set<String>, Void> callback);

  @NotNull <D extends Operation.Data> D readOperation(
      @NotNull Operation<D> operation,
      @NotNull CustomScalarAdapters customScalarAdapters,
      @NotNull CacheHeaders cacheHeaders
  );

  @NotNull <D extends Operation.Data> D readOperation(@NotNull Operation<D> operation);

  @NotNull <D extends Fragment.Data> D readFragment(
      @NotNull Fragment<D> fragment,
      @NotNull CacheKey cacheKey,
      @NotNull CustomScalarAdapters customScalarAdapters,
      @NotNull CacheHeaders cacheHeaders
  );

  @NotNull <D extends Fragment.Data> D readFragment(@NotNull Fragment<D> fragment, @NotNull CacheKey cacheKey);

  @NotNull <D extends Operation.Data> Set<String> writeOperation(
      @NotNull Operation<D> operation,
      @NotNull D operationData,
      @NotNull CustomScalarAdapters customScalarAdapters,
      @NotNull CacheHeaders cacheHeaders,
      boolean publish
  );

  @NotNull <D extends Operation.Data> Set<String> writeOperation(@NotNull Operation<D> operation, @NotNull D operationData);

  @NotNull <D extends Fragment.Data> Set<String> writeFragment(
      @NotNull Fragment<D> fragment,
      @NotNull CacheKey cacheKey,
      @NotNull D fragmentData,
      @NotNull CustomScalarAdapters customScalarAdapters,
      @NotNull CacheHeaders cacheHeaders,
      boolean publish
  );

  @NotNull <D extends Fragment.Data> Set<String> writeFragment(@NotNull Fragment<D> fragment, @NotNull CacheKey cacheKey, @NotNull D fragmentData);

  @NotNull <D extends Operation.Data> Set<String> writeOptimisticUpdates(
      @NotNull Operation<D> operation,
      @NotNull D operationData,
      @NotNull UUID mutationId,
      @NotNull CustomScalarAdapters customScalarAdapters,
      boolean publish
  );

  @NotNull <D extends Operation.Data> Set<String> writeOptimisticUpdates(@NotNull Operation<D> operation, @NotNull D operationData, @NotNull UUID mutationId);

  void rollbackOptimisticUpdates(@NotNull UUID mutationId, boolean publish);

  void rollbackOptimisticUpdates(@NotNull UUID mutationId);

  boolean clearAll();

  boolean remove(@NotNull CacheKey cacheKey, boolean cascade);

  boolean remove(@NotNull CacheKey cacheKey);

  int remove(@NotNull List<CacheKey> cacheKeys, boolean cascade);

  int remove(@NotNull List<CacheKey> cacheKeys);

  @NotNull <D extends Operation.Data> Map<String, Record> normalize(
      @NotNull Operation<D> operation,
      @NotNull D data,
      @NotNull CustomScalarAdapters customScalarAdapters
  );

  void publish(@NotNull Set<String> keys);

  <R> R accessCache(@NotNull Function<NormalizedCache, R> block);

  @NotNull Map<Class<?>, Map<String, Record>> dump();

  void dispose();

  static ApolloStore createApolloStore(NormalizedCacheFactory normalizedCacheFactory, CacheKeyGenerator cacheKeyGenerator, CacheResolver cacheResolver) {
    return ApolloStoreAdapter.createApolloStore(normalizedCacheFactory, cacheKeyGenerator, cacheResolver);
  }

  static ApolloStore createApolloStore(NormalizedCacheFactory normalizedCacheFactory) {
    return ApolloStoreAdapter.createApolloStore(normalizedCacheFactory);
  }

}
