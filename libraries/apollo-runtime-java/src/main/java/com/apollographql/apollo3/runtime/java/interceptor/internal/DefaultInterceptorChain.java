package com.apollographql.apollo.runtime.java.interceptor.internal;

import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.runtime.java.ApolloCallback;
import com.apollographql.apollo.runtime.java.ApolloDisposable;
import com.apollographql.apollo.runtime.java.interceptor.ApolloInterceptor;
import com.apollographql.apollo.runtime.java.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.runtime.java.internal.DefaultApolloDisposable;
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
