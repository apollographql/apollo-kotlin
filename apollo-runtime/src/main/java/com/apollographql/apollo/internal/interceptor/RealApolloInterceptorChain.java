package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.interceptor.FetchOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * RealApolloInterceptorChain is responsible for building the entire interceptor chain. Based on the task at hand,
 * the chain may contain interceptors which fetch responses from the normalized cache, make network calls
 * to fetch data if needed or parse the http responses to inflate models.
 */
public final class RealApolloInterceptorChain implements ApolloInterceptorChain {
  private final Operation operation;
  private final List<ApolloInterceptor> interceptors;
  private final int interceptorIndex;

  public RealApolloInterceptorChain(@Nonnull Operation operation, @Nonnull List<ApolloInterceptor> interceptors) {
    this(operation, interceptors, 0);
  }

  private RealApolloInterceptorChain(Operation operation, List<ApolloInterceptor> interceptors, int interceptorIndex) {
    if (interceptorIndex > interceptors.size()) throw new IllegalArgumentException();

    this.operation = checkNotNull(operation, "operation == null");
    this.interceptors = new ArrayList<>(checkNotNull(interceptors, "interceptors == null"));
    this.interceptorIndex = interceptorIndex;
  }

  @Override @Nonnull public ApolloInterceptor.InterceptorResponse proceed(@Nonnull FetchOptions fetchOptions)
      throws ApolloException {
    if (interceptorIndex >= interceptors.size()) throw new IllegalStateException();

    return interceptors.get(interceptorIndex).intercept(operation, new RealApolloInterceptorChain(operation,
        interceptors, interceptorIndex + 1), fetchOptions);
  }

  @Override public void proceedAsync(@Nonnull ExecutorService dispatcher, @Nonnull FetchOptions fetchOptions,
      @Nonnull ApolloInterceptor.CallBack callBack) {
    if (interceptorIndex >= interceptors.size()) throw new IllegalStateException();
    interceptors.get(interceptorIndex).interceptAsync(operation, new RealApolloInterceptorChain(operation,
        interceptors, interceptorIndex + 1), dispatcher, fetchOptions, callBack);
  }

  @Override public void dispose() {
    for (ApolloInterceptor interceptor : interceptors) {
      interceptor.dispose();
    }
  }
}
