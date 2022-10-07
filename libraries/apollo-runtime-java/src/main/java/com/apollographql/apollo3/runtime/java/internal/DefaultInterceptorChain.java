package com.apollographql.apollo3.runtime.java.internal;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.ApolloDisposable;
import com.apollographql.apollo3.runtime.java.ApolloInterceptor;
import com.apollographql.apollo3.runtime.java.ApolloInterceptorChain;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class DefaultInterceptorChain implements ApolloInterceptorChain {
  private ArrayList<ApolloInterceptor> interceptors;
  private int index;
  private DefaultApolloDisposable disposable;

  public DefaultInterceptorChain(
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
