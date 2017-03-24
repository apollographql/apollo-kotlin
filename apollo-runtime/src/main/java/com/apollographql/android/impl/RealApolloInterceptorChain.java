package com.apollographql.android.impl;

import com.apollographql.android.api.graphql.Operation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;

final class RealApolloInterceptorChain implements ApolloInterceptorChain {
  private final Operation operation;
  private final List<ApolloInterceptor> interceptors;
  private final int interceptorIndex;

  RealApolloInterceptorChain(@Nonnull Operation operation, @Nonnull List<ApolloInterceptor> interceptors) {
    this(operation, interceptors, 0);
  }

  private RealApolloInterceptorChain(Operation operation, List<ApolloInterceptor> interceptors, int interceptorIndex) {
    if (interceptorIndex > interceptors.size()) throw new IllegalArgumentException();

    this.operation = checkNotNull(operation, "operation == null");
    this.interceptors = new ArrayList<>(checkNotNull(interceptors, "interceptors == null"));
    this.interceptorIndex = interceptorIndex;
  }

  @Override @Nonnull public ApolloInterceptor.InterceptorResponse proceed() throws IOException {
    if (interceptorIndex >= interceptors.size()) throw new IllegalStateException();

    return interceptors.get(interceptorIndex).intercept(operation, new RealApolloInterceptorChain(operation,
        interceptors, interceptorIndex + 1));
  }

  @Override public void proceedAsync(@Nonnull ExecutorService dispatcher,
      @Nonnull ApolloInterceptor.CallBack callBack) {
    if (interceptorIndex >= interceptors.size()) throw new IllegalStateException();

    interceptors.get(interceptorIndex).interceptAsync(operation, new RealApolloInterceptorChain(operation,
        interceptors, interceptorIndex + 1), dispatcher, callBack);
  }

  @Override public void dispose() {
    for (ApolloInterceptor interceptor : interceptors) {
      interceptor.dispose();
    }
  }
}
