package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.cache.normalized.Transaction;
import com.apollographql.apollo.internal.cache.normalized.WriteableStore;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * ApolloCacheInterceptor is a concrete {@link ApolloInterceptor} responsible for serving requests from the normalized
 * cache if {@link InterceptorRequest#fetchFromCache} is true. Saves all network responses to cache.
 */
public final class ApolloCacheInterceptor implements ApolloInterceptor {
  private final ApolloStore apolloStore;
  private final ResponseFieldMapper responseFieldMapper;
  private final Executor dispatcher;
  private final ApolloLogger logger;
  private volatile boolean disposed;

  public ApolloCacheInterceptor(@Nonnull ApolloStore apolloStore, @Nonnull ResponseFieldMapper responseFieldMapper,
      @Nonnull Executor dispatcher, @Nonnull ApolloLogger logger) {
    this.apolloStore = checkNotNull(apolloStore, "cache == null");
    this.responseFieldMapper = checkNotNull(responseFieldMapper, "responseFieldMapper == null");
    this.dispatcher = checkNotNull(dispatcher, "dispatcher == null");
    this.logger = checkNotNull(logger, "logger == null");
  }

  @Override
  public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull final ApolloInterceptorChain chain,
      @Nonnull final Executor dispatcher, @Nonnull final CallBack callBack) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        if (disposed) return;
        if (request.fetchFromCache) {
          callBack.onFetch(FetchSourceType.CACHE);
          final InterceptorResponse cachedResponse;
          try {
            cachedResponse = resolveFromCache(request);
            callBack.onResponse(cachedResponse);
            callBack.onCompleted();
          } catch (ApolloException e) {
            callBack.onFailure(e);
          }
        } else {
          writeOptimisticUpdatesAndPublish(request);
          chain.proceedAsync(request, dispatcher, new CallBack() {
            @Override public void onResponse(@Nonnull InterceptorResponse networkResponse) {
              if (disposed) return;

              try {
                Set<String> networkResponseCacheKeys = cacheResponse(networkResponse, request);
                Set<String> rolledBackCacheKeys = rollbackOptimisticUpdates(request);
                Set<String> changedCacheKeys = new HashSet<>();
                changedCacheKeys.addAll(rolledBackCacheKeys);
                changedCacheKeys.addAll(networkResponseCacheKeys);
                publishCacheKeys(changedCacheKeys);
              } catch (Exception rethrow) {
                rollbackOptimisticUpdatesAndPublish(request);
                throw rethrow;
              }

              callBack.onResponse(networkResponse);
              callBack.onCompleted();
            }

            @Override public void onFailure(@Nonnull ApolloException t) {
              rollbackOptimisticUpdatesAndPublish(request);
              callBack.onFailure(t);
            }

            @Override public void onCompleted() {
            }

            @Override public void onFetch(FetchSourceType sourceType) {
              callBack.onFetch(sourceType);
            }
          });
        }
      }
    });
  }

  @Override public void dispose() {
    disposed = true;
  }

  private InterceptorResponse resolveFromCache(InterceptorRequest request) throws ApolloException {
    ResponseNormalizer<Record> responseNormalizer = apolloStore.cacheResponseNormalizer();
    //noinspection unchecked
    ApolloStoreOperation<Response> apolloStoreOperation = apolloStore.read(request.operation, responseFieldMapper,
        responseNormalizer, request.cacheHeaders);
    Response cachedResponse = apolloStoreOperation.execute();
    if (cachedResponse.data() != null) {
      logger.d("Cache HIT for operation %s", request.operation);
      return new InterceptorResponse(null, cachedResponse, responseNormalizer.records());
    }
    logger.d("Cache MISS for operation %s", request.operation);
    throw new ApolloException(String.format("Cache miss for operation %s", request.operation));
  }

  private Set<String> cacheResponse(final InterceptorResponse networkResponse,
      final InterceptorRequest request) {
    final Optional<List<Record>> records = networkResponse.cacheRecords.map(
        new Function<Collection<Record>, List<Record>>() {
          @Nonnull @Override public List<Record> apply(@Nonnull Collection<Record> records) {
            final List<Record> result = new ArrayList<>(records.size());
            for (Record record : records) {
              result.add(record.toBuilder().mutationId(request.uniqueId).build());
            }
            return result;
          }
        }
    );

    if (!records.isPresent()) {
      return Collections.emptySet();
    }

    try {
      return apolloStore.writeTransaction(new Transaction<WriteableStore, Set<String>>() {
        @Nullable @Override public Set<String> execute(WriteableStore cache) {
          return cache.merge(records.get(), request.cacheHeaders);
        }
      });
    } catch (Exception e) {
      logger.e("Failed to cache operation response", e);
      return Collections.emptySet();
    }
  }

  private void writeOptimisticUpdatesAndPublish(final InterceptorRequest request) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        try {
          if (request.optimisticUpdates.isPresent()) {
            Operation.Data optimisticUpdates = request.optimisticUpdates.get();
            apolloStore.writeOptimisticUpdatesAndPublish(request.operation, optimisticUpdates, request.uniqueId)
                .execute();
          }
        } catch (Exception e) {
          logger.e(e, "failed to write operation optimistic updates, for: %s", request.operation);
        }
      }
    });
  }

  private void rollbackOptimisticUpdatesAndPublish(final InterceptorRequest request) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        try {
          apolloStore.rollbackOptimisticUpdatesAndPublish(request.uniqueId).execute();
        } catch (Exception e) {
          logger.e(e, "failed to rollback operation optimistic updates, for: %s", request.operation);
        }
      }
    });
  }

  private Set<String> rollbackOptimisticUpdates(final InterceptorRequest request) {
    try {
      return apolloStore.rollbackOptimisticUpdates(request.uniqueId).execute();
    } catch (Exception e) {
      logger.e(e, "failed to rollback operation optimistic updates, for: %s", request.operation);
      return Collections.emptySet();
    }
  }

  private void publishCacheKeys(final Set<String> cacheKeys) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        try {
          apolloStore.publish(cacheKeys);
        } catch (Exception e) {
          logger.e(e, "Failed to publish cache changes");
        }
      }
    });
  }
}
