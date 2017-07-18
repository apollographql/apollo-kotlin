package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
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
 * Signals the apollo client to <b>only</b> fetch the data from the normalized cache. If it's not present in
 * the normalized cache or if an exception occurs while trying to fetch it from the normalized cache, an empty {@link
 * com.apollographql.apollo.api.Response} is sent back with the {@link com.apollographql.apollo.api.Operation} info
 * wrapped inside.
 */
public final class CacheOnlyFetcher implements ResponseFetcher {

  @Override public ApolloInterceptor provideInterceptor(ApolloLogger apolloLogger) {
    return new CacheOnlyInterceptor();
  }

  private static final class CacheOnlyInterceptor implements ApolloInterceptor {

    private volatile boolean disposed;

    @Nonnull @Override
    public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
        @Nonnull FetchOptions options) throws ApolloException {
      if (disposed) throw new ApolloCanceledException("Canceled");
      FetchOptions cacheFetchOptions = options.toCacheFetchOptions();
      try {
        return chain.proceed(cacheFetchOptions);
      } catch (Exception e) {
        return cacheMissResponse(operation);
      }
    }

    @Override
    public void interceptAsync(@Nonnull final Operation operation, @Nonnull ApolloInterceptorChain chain,
        @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions options, @Nonnull final CallBack callBack) {
      FetchOptions cacheFetchOptions = options.toCacheFetchOptions();
      chain.proceedAsync(dispatcher, cacheFetchOptions, new CallBack() {
        @Override public void onResponse(@Nonnull InterceptorResponse response) {
          callBack.onResponse(response);
        }

        @Override public void onFailure(@Nonnull ApolloException e) {
          // Cache only returns null instead of throwing when the cache is empty
          callBack.onResponse(cacheMissResponse(operation));
          callBack.onCompleted();
        }

        @Override public void onCompleted() {
          callBack.onCompleted();
        }
      });
    }

    @Override public void dispose() {
      //no-op
    }

    private InterceptorResponse cacheMissResponse(Operation operation) {
      return new InterceptorResponse(null, Response.builder(operation).fromCache(true).build(), null);
    }
  }

}
