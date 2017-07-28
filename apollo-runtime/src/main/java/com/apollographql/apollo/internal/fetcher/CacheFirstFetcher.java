package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

/**
 * Signals the apollo client to first fetch the data from the normalized cache. If it's not present in the normalized
 * cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is instead fetched
 * from the network.
 */
public final class CacheFirstFetcher implements ResponseFetcher {

  @Override public ApolloInterceptor provideInterceptor(final ApolloLogger apolloLogger) {
    return new CacheFirstInterceptor();
  }

  private static final class CacheFirstInterceptor implements ApolloInterceptor {

    private volatile boolean disposed;

    @Nonnull @Override
    public InterceptorResponse intercept(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain)
        throws ApolloException {
      if (disposed) throw new ApolloCanceledException("Canceled");

      InterceptorResponse response;
      InterceptorRequest cacheRequest = request.withFetchOptions(request.fetchOptions.toCacheFetchOptions());
      try {
        response = chain.proceed(cacheRequest);
      } catch (ApolloException exception) {
        InterceptorRequest networkRequest = request.withFetchOptions(request.fetchOptions.toNetworkFetchOptions());
        response = chain.proceed(networkRequest);
      }
      return response;
    }

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull final ApolloInterceptorChain chain,
        @Nonnull final ExecutorService dispatcher, @Nonnull final CallBack callBack) {
      InterceptorRequest cacheRequest = request.withFetchOptions(request.fetchOptions.toCacheFetchOptions());
      chain.proceedAsync(cacheRequest, dispatcher, new CallBack() {
        @Override public void onResponse(@Nonnull InterceptorResponse response) {
          callBack.onResponse(response);
        }

        @Override public void onFailure(@Nonnull ApolloException e) {
          if (!disposed) {
            InterceptorRequest networkRequest = request.withFetchOptions(request.fetchOptions.toNetworkFetchOptions());
            chain.proceedAsync(networkRequest, dispatcher, callBack);
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