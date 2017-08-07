package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.Function;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.interceptor.FetchOptions;
import com.apollographql.apollo.internal.cache.normalized.ResponseNormalizer;
import com.apollographql.apollo.internal.cache.normalized.Transaction;
import com.apollographql.apollo.internal.cache.normalized.WriteableStore;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * ApolloCacheInterceptor is a concrete {@link ApolloInterceptor} responsible for serving requests from the normalized
 * cache if {@link FetchOptions#fetchFromCache} is true. Saves all network responses to cache.
 */
public final class ApolloCacheInterceptor implements ApolloInterceptor {
  private final ApolloStore apolloStore;
  private final ResponseFieldMapper responseFieldMapper;
  private final ExecutorService dispatcher;
  private final ApolloLogger logger;
  private volatile boolean disposed;

  public ApolloCacheInterceptor(@Nonnull ApolloStore apolloStore, @Nonnull ResponseFieldMapper responseFieldMapper,
      @Nonnull ExecutorService dispatcher, @Nonnull ApolloLogger logger) {
    this.apolloStore = checkNotNull(apolloStore, "cache == null");
    this.responseFieldMapper = checkNotNull(responseFieldMapper, "responseFieldMapper == null");
    this.dispatcher = checkNotNull(dispatcher, "dispatcher == null");
    this.logger = checkNotNull(logger, "logger == null");
  }

  @Nonnull @Override
  public InterceptorResponse intercept(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain)
      throws ApolloException {
    if (disposed) throw new ApolloCanceledException("Canceled");
    if (request.fetchOptions.fetchFromCache) {
      return resolveFromCache(request.operation, request.fetchOptions);
    }
    InterceptorResponse networkResponse = chain.proceed(request);
    cacheResponse(networkResponse, request);
    return networkResponse;
  }

  @Override
  public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull final ApolloInterceptorChain chain,
      @Nonnull final ExecutorService dispatcher, @Nonnull final CallBack callBack) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        if (disposed) return;
        if (request.fetchOptions.fetchFromCache) {
          final InterceptorResponse cachedResponse;
          try {
            cachedResponse = resolveFromCache(request.operation, request.fetchOptions);
            callBack.onResponse(cachedResponse);
            callBack.onCompleted();
          } catch (ApolloException e) {
            callBack.onFailure(e);
          }
        } else {
          chain.proceedAsync(request, dispatcher, new CallBack() {
            @Override public void onResponse(@Nonnull InterceptorResponse networkResponse) {
              if (disposed) return;
              cacheResponse(networkResponse, request);
              callBack.onResponse(networkResponse);
              callBack.onCompleted();
            }

            @Override public void onFailure(@Nonnull ApolloException e) {
              callBack.onFailure(e);
            }

            @Override public void onCompleted() {
            }
          });
        }
      }
    });
  }

  @Override public void dispose() {
    disposed = true;
  }

  private InterceptorResponse resolveFromCache(Operation operation, FetchOptions options) throws ApolloException {
    ResponseNormalizer<Record> responseNormalizer = apolloStore.cacheResponseNormalizer();
    //noinspection unchecked
    ApolloStoreOperation<Response> apolloStoreOperation = apolloStore.read(operation, responseFieldMapper,
        responseNormalizer, options.cacheHeaders);
    Response cachedResponse = apolloStoreOperation.execute();
    if (cachedResponse.data() != null) {
      logger.d("Cache HIT for operation %s", operation);
      return new InterceptorResponse(null, cachedResponse, responseNormalizer.records());
    }
    logger.d("Cache MISS for operation %s", operation);
    throw new ApolloException(String.format("Cache miss for operation %s", operation));
  }

  private void cacheResponse(final InterceptorResponse networkResponse,
      final ApolloInterceptor.InterceptorRequest request) {
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
      return;
    }

    final Set<String> changedKeys;
    try {
      changedKeys = apolloStore.writeTransaction(new Transaction<WriteableStore, Set<String>>() {
        @Nullable @Override public Set<String> execute(WriteableStore cache) {
          return cache.merge(records.get(), request.fetchOptions.cacheHeaders);
        }
      });
    } catch (Exception e) {
      logger.e("Failed to cache operation response", e);
      return;
    }

    dispatcher.execute(new Runnable() {
      @Override public void run() {
        try {
          apolloStore.publish(changedKeys);
        } catch (Exception e) {
          logger.e("Failed to publish cache changes", e);
        }
      }
    });
  }
}
