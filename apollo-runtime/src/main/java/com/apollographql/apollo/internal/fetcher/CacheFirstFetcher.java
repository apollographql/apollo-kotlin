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
 * Signals the apollo client to first fetch the data from the normalized cache. If it's not present in the
 * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is
 * instead fetched from the network.
 */
public final class CacheFirstFetcher implements ResponseFetcher {

  @Override public ApolloInterceptor provideInterceptor(final ApolloLogger apolloLogger) {
    return new CacheFirstInterceptor();
  }

  private static final class CacheFirstInterceptor implements ApolloInterceptor {

    private volatile boolean disposed;

    @Nonnull @Override
    public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
        @Nonnull FetchOptions options) throws ApolloException {
      if (disposed) throw new ApolloCanceledException("Canceled");
      FetchOptions cacheFetchOptions = options.toCacheFetchOptions();
      InterceptorResponse response;
      try {
        response = chain.proceed(cacheFetchOptions);
      } catch (ApolloException exception) {
        response = chain.proceed(options.toNetworkFetchOptions());
      }
      return response;
    }

    @Override
    public void interceptAsync(@Nonnull final Operation operation, @Nonnull final ApolloInterceptorChain chain,
        @Nonnull final ExecutorService dispatcher, @Nonnull final FetchOptions options,
        @Nonnull final CallBack callBack) {
      chain.proceedAsync(dispatcher, options.toCacheFetchOptions(), new CallBack() {
        @Override public void onResponse(@Nonnull InterceptorResponse response) {
          callBack.onResponse(response);
        }

        @Override public void onFailure(@Nonnull ApolloException e) {
          if (!disposed) {
            chain.proceedAsync(dispatcher, options.toNetworkFetchOptions(), callBack);
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
