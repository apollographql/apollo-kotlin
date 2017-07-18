package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.interceptor.FetchOptions;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

/**
 * Signals the apollo client to first fetch the data from the network. If network request fails, then the
 * data is fetched from the normalized cache. If the data is not present in the normalized cache, then the
 * exception which led to the network request failure is rethrown.
 */
public final class NetworkFirstFetcher implements ResponseFetcher {

  @Override public ApolloInterceptor provideInterceptor(ApolloLogger logger) {
    return new NetworkFirstInterceptor(logger);
  }

  private static final class NetworkFirstInterceptor implements ApolloInterceptor {
    private volatile boolean disposed;
    private final ApolloLogger logger;

    NetworkFirstInterceptor(ApolloLogger logger) {
      this.logger = logger;
    }

    @Nonnull @Override
    public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
        @Nonnull FetchOptions options) throws ApolloException {
      if (disposed) throw new ApolloCanceledException("Canceled");
      try {
        return chain.proceed(options.toNetworkFetchOptions());
      } catch (ApolloException e) {
        InterceptorResponse networkFirstCacheResponse = chain.proceed(options.toCacheFetchOptions());
        if (networkFirstCacheResponse.parsedResponse.isPresent()) {
          logger.d(e, "Failed to fetch network response for operation %s, return cached one", operation);
          return networkFirstCacheResponse;
        }
        throw e;
      }
    }

    @Override
    public void interceptAsync(@Nonnull final Operation operation, @Nonnull final ApolloInterceptorChain chain,
        @Nonnull final ExecutorService dispatcher, @Nonnull final FetchOptions options,
        @Nonnull final CallBack callBack) {
      chain.proceedAsync(dispatcher, options.toNetworkFetchOptions(), new CallBack() {
        @Override public void onResponse(@Nonnull InterceptorResponse response) {
          callBack.onResponse(response);
        }

        @Override public void onFailure(@Nonnull ApolloException e) {
          logger.d(e, "Failed to fetch network response for operation %s, trying to return cached one", operation);
          if (!disposed) {
            chain.proceedAsync(dispatcher, options.toCacheFetchOptions(), callBack);
          }
        }

        @Override public void onCompleted() {
          callBack.onCompleted();
        }
      });

    }

    @Override public void dispose() {
      disposed = true;
    }
  }


}
