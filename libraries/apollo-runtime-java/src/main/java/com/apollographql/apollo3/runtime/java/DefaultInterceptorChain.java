package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptorChain;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

class DefaultInterceptorChain implements ApolloInterceptorChain {
  private ArrayList<ApolloInterceptor> interceptors;
  private int index;
  private DefaultApolloDisposable disposable;

  DefaultInterceptorChain(
      ArrayList<ApolloInterceptor> interceptors,
      int index,
      DefaultApolloDisposable disposable
  ) {
    this.index = index;
    this.interceptors = interceptors;
    this.disposable = disposable;
  }

  @Override public ApolloDisposable getDisposable() {
    return disposable;
  }

  @Override public <D extends Operation.Data> void proceed(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callBack) {
    ApolloInterceptor apolloInterceptor = interceptors.get(index);
    apolloInterceptor.intercept(
        request,
        new DefaultInterceptorChain(interceptors, index + 1, disposable),
        callBack
    );
  }
}
