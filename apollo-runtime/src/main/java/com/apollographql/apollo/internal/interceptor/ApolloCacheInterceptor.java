package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.ApolloStore;
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

import java.util.Collection;
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
  public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
      @Nonnull FetchOptions options) throws ApolloException {
    if (disposed) throw new ApolloCanceledException("Canceled");
    if (options.fetchFromCache) {
      return resolveFromCache(operation, options);
    }
    InterceptorResponse networkResponse = chain.proceed(options);
    cacheResponse(networkResponse, options);
    return networkResponse;
  }

  @Override
  public void interceptAsync(@Nonnull final Operation operation, @Nonnull final ApolloInterceptorChain chain,
      @Nonnull final ExecutorService dispatcher, @Nonnull final FetchOptions options,
      @Nonnull final CallBack callBack) {
    dispatcher.execute(new Runnable() {
      @Override public void run() {
        if (disposed) return;
        if (options.fetchFromCache) {
          final InterceptorResponse cachedResponse;
          try {
            cachedResponse = resolveFromCache(operation, options);
            callBack.onResponse(cachedResponse);
            callBack.onCompleted();
          } catch (ApolloException e) {
            callBack.onFailure(e);
          }
        } else {
          chain.proceedAsync(dispatcher, options, new CallBack() {
            @Override public void onResponse(@Nonnull InterceptorResponse networkResponse) {
              if (disposed) return;
              cacheResponse(networkResponse, options);
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

      Response cachedResponse = apolloStore.read(operation, responseFieldMapper, responseNormalizer,
          options.cacheHeaders);
    if (cachedResponse.data() != null) {
      logger.d("Cache HIT for operation %s", operation);
      return new InterceptorResponse(null, cachedResponse, responseNormalizer.records());
    }
    logger.d("Cache MISS for operation %s", operation);
    throw new ApolloException(String.format("Cache miss for operation %s", operation));
  }

  private void cacheResponse(final InterceptorResponse networkResponse, final FetchOptions options) {
    final Optional<Collection<Record>> records = networkResponse.cacheRecords;
    if (!records.isPresent()) {
      return;
    }

    final Set<String> changedKeys;
    try {
      changedKeys = apolloStore.writeTransaction(new Transaction<WriteableStore, Set<String>>() {
        @Nullable @Override public Set<String> execute(WriteableStore cache) {
          return cache.merge(records.get(), options.cacheHeaders);
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
