package com.apollographql.apollo.internal.interceptor;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

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

  @Override @Nonnull public ApolloInterceptor.InterceptorResponse proceed() throws ApolloException {
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
