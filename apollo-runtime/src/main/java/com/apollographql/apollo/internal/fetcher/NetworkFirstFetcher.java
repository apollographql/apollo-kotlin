package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * Signals the apollo client to first fetch the data from the network. If network request fails, then the data is
 * fetched from the normalized cache. If the data is not present in the normalized cache, then the exception which led
 * to the network request failure is rethrown.
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

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull final ApolloInterceptorChain chain,
        @Nonnull final Executor dispatcher, @Nonnull final CallBack callBack) {
      InterceptorRequest networkRequest = request.withFetchOptions(request.fetchOptions.toNetworkFetchOptions());
      chain.proceedAsync(networkRequest, dispatcher, new CallBack() {
        @Override public void onResponse(@Nonnull InterceptorResponse response) {
          callBack.onResponse(response);
        }

        @Override public void onFailure(@Nonnull ApolloException e) {
          logger.d(e, "Failed to fetch network response for operation %s, trying to return cached one",
              request.operation);
          if (!disposed) {
            InterceptorRequest cacheRequest = request.withFetchOptions(request.fetchOptions.toCacheFetchOptions());
            chain.proceedAsync(cacheRequest, dispatcher, callBack);
          }
        }

        @Override public void onCompleted() {
          callBack.onCompleted();
        }

        @Override public void onFetch(FetchSourceType sourceType) {
            callBack.onFetch(sourceType);
          }
      });

    }

    @Override public void dispose() {
      disposed = true;
    }
  }
}
