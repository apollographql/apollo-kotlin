package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.cache.http.HttpCache;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;
import com.apollographql.apollo.internal.util.ApolloLogger;
import com.squareup.moshi.Moshi;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Response;

@SuppressWarnings("WeakerAccess") public final class RealApolloPrefetch implements ApolloPrefetch {
  final Operation operation;
  final HttpUrl serverUrl;
  final Call.Factory httpCallFactory;
  final HttpCache httpCache;
  final Moshi moshi;
  final ExecutorService dispatcher;
  final ApolloLogger logger;
  final ApolloCallTracker tracker;
  final ApolloInterceptorChain interceptorChain;
  volatile boolean executed;
  volatile boolean canceled;

  public RealApolloPrefetch(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      Moshi moshi, ExecutorService dispatcher, ApolloLogger logger, ApolloCallTracker callTracker) {
    this.operation = operation;
    this.serverUrl = serverUrl;
    this.httpCallFactory = httpCallFactory;
    this.httpCache = httpCache;
    this.moshi = moshi;
    this.dispatcher = dispatcher;
    this.logger = logger;
    this.tracker = callTracker;
    interceptorChain = new RealApolloInterceptorChain(operation, Collections.<ApolloInterceptor>singletonList(
        new ApolloServerInterceptor(serverUrl, httpCallFactory, HttpCachePolicy.NETWORK_ONLY, true, moshi,
            logger)
    ));
  }

  @Override public void execute() throws ApolloException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    if (canceled) {
      throw new ApolloCanceledException("Canceled");
    }

    Response httpResponse;

    try {
      tracker.onSyncPrefetchInProgress(this);
      httpResponse = interceptorChain.proceed().httpResponse.get();
    } catch (Exception e) {
      if (canceled) {
        throw new ApolloCanceledException("Canceled", e);
      } else {
        throw e;
      }
    } finally {
      tracker.onSyncPrefetchFinished(this);
    }

    httpResponse.close();

    if (canceled) {
      throw new ApolloCanceledException("Canceled");
    }

    if (!httpResponse.isSuccessful()) {
      throw new ApolloHttpException(httpResponse);
    }
  }

  @Nonnull @Override public ApolloPrefetch enqueue(@Nullable final Callback responseCallback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    AsyncCall asyncCall = new AsyncCall(responseCallback);
    tracker.onAsyncPrefetchInProgress(asyncCall);
    interceptorChain.proceedAsync(dispatcher, asyncCall);
    return this;
  }

  class AsyncCall implements ApolloInterceptor.CallBack {

    private final Callback responseCallback;

    private AsyncCall(Callback responseCallback) {
      this.responseCallback = responseCallback;
    }

    @Override public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
      try {
        if (responseCallback == null) return;

        Response httpResponse = response.httpResponse.get();
        httpResponse.close();

        if (canceled) {
          responseCallback.onCanceledError(new ApolloCanceledException("Canceled"));
        }

        if (httpResponse.isSuccessful()) {
          responseCallback.onSuccess();
        } else {
          responseCallback.onHttpError(new ApolloHttpException(httpResponse));
        }
      } finally {
        tracker.onAsyncPrefetchFinished(this);
      }
    }

    @Override public void onFailure(@Nonnull ApolloException e) {
      try {

        if (responseCallback == null) {
          return;
        }

        if (canceled) {
          responseCallback.onCanceledError(new ApolloCanceledException("Canceled"));
        } else if (e instanceof ApolloHttpException) {
          responseCallback.onHttpError((ApolloHttpException) e);
        } else if (e instanceof ApolloNetworkException) {
          responseCallback.onNetworkError((ApolloNetworkException) e);
        } else {
          responseCallback.onFailure(e);
        }

      } finally {
        tracker.onAsyncPrefetchFinished(this);
      }
    }
  }

  @Override public ApolloPrefetch clone() {
    return new RealApolloPrefetch(operation, serverUrl, httpCallFactory, httpCache, moshi, dispatcher, logger, tracker);
  }

  @Override public void cancel() {
    canceled = true;
    interceptorChain.dispose();
  }

  @Override public boolean isCanceled() {
    return canceled;
  }
}
