package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloInterceptorChain;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

class DefaultInterceptorChain<D extends Operation.Data> implements ApolloInterceptorChain<D> {
  private ArrayList<ApolloInterceptor<D>> interceptors;
  private int index;
  private DefaultApolloDisposable disposable;

  DefaultInterceptorChain(
      ArrayList<ApolloInterceptor<D>> interceptors,
      int index,
      DefaultApolloDisposable disposable
  ) {
    this.index = index;
    this.interceptors = interceptors;
    this.disposable = disposable;
  }

  @Override public boolean isDisposed() {
    return disposable.isDisposed();
  }

  @Override public void proceed(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callBack) {
    interceptors.get(index).intercept(
        request,
        new DefaultInterceptorChain<>(interceptors, index + 1, disposable),
        callBack
    );
  }
}
