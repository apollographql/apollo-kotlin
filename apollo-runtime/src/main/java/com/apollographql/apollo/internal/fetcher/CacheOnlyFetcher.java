package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.util.ApolloLogger;

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

/**
 * Signals the apollo client to <b>only</b> fetch the data from the normalized cache. If it's not present in the
 * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, an empty {@link
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
    public InterceptorResponse intercept(@Nonnull InterceptorRequest request, @Nonnull ApolloInterceptorChain chain)
        throws ApolloException {
      if (disposed) throw new ApolloCanceledException("Canceled");

      InterceptorRequest cacheRequest = request.withFetchOptions(request.fetchOptions.toCacheFetchOptions());
      try {
        return chain.proceed(cacheRequest);
      } catch (Exception e) {
        return cacheMissResponse(request.operation);
      }
    }

    @Override
    public void interceptAsync(@Nonnull final InterceptorRequest request, @Nonnull ApolloInterceptorChain chain,
        @Nonnull ExecutorService dispatcher, @Nonnull final CallBack callBack) {
      InterceptorRequest cacheRequest = request.withFetchOptions(request.fetchOptions.toCacheFetchOptions());
      chain.proceedAsync(cacheRequest, dispatcher, new CallBack() {
        @Override public void onResponse(@Nonnull InterceptorResponse response) {
          callBack.onResponse(response);
        }

        @Override public void onFailure(@Nonnull ApolloException e) {
          // Cache only returns null instead of throwing when the cache is empty
          callBack.onResponse(cacheMissResponse(request.operation));
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