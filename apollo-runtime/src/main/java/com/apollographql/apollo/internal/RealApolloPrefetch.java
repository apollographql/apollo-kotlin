package com.apollographql.apollo.internal;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.cache.http.HttpCacheControl;
import com.apollographql.apollo.internal.cache.http.HttpCache;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;

@SuppressWarnings("WeakerAccess") public final class RealApolloPrefetch implements ApolloPrefetch {
  final Operation operation;
  final HttpUrl serverUrl;
  final Call.Factory httpCallFactory;
  final HttpCache httpCache;
  final Moshi moshi;
  final ExecutorService dispatcher;
  final ApolloInterceptorChain interceptorChain;
  volatile boolean executed;

  public RealApolloPrefetch(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      Moshi moshi, ExecutorService dispatcher) {
    this.operation = operation;
    this.serverUrl = serverUrl;
    this.httpCallFactory = httpCallFactory;
    this.httpCache = httpCache;
    this.moshi = moshi;
    this.dispatcher = dispatcher;
    interceptorChain = new RealApolloInterceptorChain(operation, Collections.<ApolloInterceptor>singletonList(
        new ApolloServerInterceptor(serverUrl, httpCallFactory, HttpCacheControl.NETWORK_FIRST, true, moshi)
    ));
  }

  @Override public void execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    interceptorChain.proceed().httpResponse.get().close();
  }

  @Nonnull @Override public ApolloPrefetch enqueue(@Nullable final Callback callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    interceptorChain.proceedAsync(dispatcher, new ApolloInterceptor.CallBack() {
      @Override public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        try {
          response.httpResponse.get().close();
          if (callback != null) {
            callback.onSuccess();
          }
        } catch (Exception e) {
          onFailure(e);
        }
      }

      @Override public void onFailure(@Nonnull Throwable t) {
        if (callback != null) {
          callback.onFailure(t);
        }
      }
    });
    return this;
  }

  @Override public ApolloPrefetch clone() {
    return new RealApolloPrefetch(operation, serverUrl, httpCallFactory, httpCache, moshi, dispatcher);
  }

  @Override public void cancel() {
    interceptorChain.dispose();
  }
}
