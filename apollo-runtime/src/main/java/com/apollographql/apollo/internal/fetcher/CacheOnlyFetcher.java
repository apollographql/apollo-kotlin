package com.apollographql.apollo.internal.fetcher;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.fetcher.ResponseFetcher;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.ApolloLogger;

import java.util.concurrent.Executor;

import org.jetbrains.annotations.NotNull;

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

    @Override
    public void interceptAsync(@NotNull final InterceptorRequest request, @NotNull ApolloInterceptorChain chain,
        @NotNull Executor dispatcher, @NotNull final CallBack callBack) {
      InterceptorRequest cacheRequest = request.toBuilder().fetchFromCache(true).build();
      chain.proceedAsync(cacheRequest, dispatcher, new CallBack() {
        @Override public void onResponse(@NotNull InterceptorResponse response) {
          callBack.onResponse(response);
        }

        @Override public void onFailure(@NotNull ApolloException e) {
          // Cache only returns null instead of throwing when the cache is empty
          callBack.onResponse(cacheMissResponse(request.operation));
          callBack.onCompleted();
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
      //no-op
    }

    InterceptorResponse cacheMissResponse(Operation operation) {
      return new InterceptorResponse(null, Response.builder(operation).fromCache(true).build(), null);
    }
  }
}