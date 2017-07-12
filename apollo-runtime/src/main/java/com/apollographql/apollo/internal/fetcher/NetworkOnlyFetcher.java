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
 * Signals the apollo client to <b>only</b> fetch the GraphQL data from the network. If network request fails, an
 * exception is thrown.
 */
public final class NetworkOnlyFetcher implements ResponseFetcher {

  @Override public ApolloInterceptor provideInterceptor(ApolloLogger apolloLogger) {
    return new NetworkOnlyInterceptor();
  }

  private static final class NetworkOnlyInterceptor implements ApolloInterceptor {

    private volatile boolean disposed;

    @Nonnull @Override
    public InterceptorResponse intercept(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
        @Nonnull FetchOptions options) throws ApolloException {
      if (disposed) throw new ApolloCanceledException("Canceled");
      return chain.proceed(options.toNetworkFetchOptions());
    }

    @Override
    public void interceptAsync(@Nonnull Operation operation, @Nonnull ApolloInterceptorChain chain,
        @Nonnull ExecutorService dispatcher, @Nonnull FetchOptions options, @Nonnull CallBack callBack) {
      chain.proceedAsync(dispatcher, options.toNetworkFetchOptions(), callBack);
    }

    @Override public void dispose() {
      disposed = true;
    }
  }
}
