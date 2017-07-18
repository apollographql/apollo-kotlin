package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloPrefetch;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.http.HttpCachePolicy;
import com.apollographql.apollo.exception.ApolloCanceledException;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.interceptor.FetchOptions;
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
  volatile boolean completed;
  Optional<Callback> callback = Optional.absent();

  public RealApolloPrefetch(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      Moshi moshi, ExecutorService dispatcher, ApolloLogger logger, ApolloCallTracker
      callTracker) {
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
      tracker.registerPrefetchCall(this);
      httpResponse = interceptorChain.proceed(FetchOptions.NETWORK_ONLY).httpResponse.get();
    } catch (Exception e) {
      if (canceled) {
        throw new ApolloCanceledException("Canceled", e);
      } else {
        throw e;
      }
    } finally {
      tracker.unregisterPrefetchCall(this);
    }

    httpResponse.close();

    if (canceled) {
      throw new ApolloCanceledException("Canceled");
    }

    if (!httpResponse.isSuccessful()) {
      throw new ApolloHttpException(httpResponse);
    }
  }

  @Override public void enqueue(@Nullable final Callback responseCallback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    this.callback = Optional.fromNullable(responseCallback);
    if (canceled) {
      if (callback.isPresent()) {
        callback.get().onCanceledError(new ApolloCanceledException("Canceled"));
        return;
      } else {
        throw new IllegalStateException("Prefetch was canceled");
      }
    }
    tracker.registerPrefetchCall(this);
    interceptorChain.proceedAsync(dispatcher, FetchOptions.NETWORK_ONLY, interceptorCallbackProxy());
  }

  @Nonnull @Override public Operation operation() {
    return operation;
  }

  private ApolloInterceptor.CallBack interceptorCallbackProxy() {
    return new ApolloInterceptor.CallBack() {
      @Override public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        synchronized (RealApolloPrefetch.this) {
          if (completed) {
            throw new IllegalStateException("onResponse called after onCompleted for prefetch of operation: "
                + operation.name().name());
          }
          Response httpResponse = response.httpResponse.get();
          try {
            if (!callback.isPresent()) {
              logger.d("onResponse for operation: %s. No callback present. Successful: %b",
                  operation.name().name(), httpResponse.isSuccessful());
              return;
            }
            if (RealApolloPrefetch.this.canceled) return;

            if (httpResponse.isSuccessful()) {
              callback.get().onSuccess();
            } else {
              callback.get().onHttpError(new ApolloHttpException(httpResponse));
            }
          } finally {
            tracker.unregisterPrefetchCall(RealApolloPrefetch.this);
            httpResponse.close();
          }
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        synchronized (RealApolloPrefetch.this){
          if (!callback.isPresent()) {
            logger.d(e, "onFailure for prefetch of operation: %s. No callback present.", operation.name().name());
            return;
          }
          try {
            if (canceled) {
              logger.w(e, "Call was canceled, but experienced exception.");
            } else if (e instanceof ApolloHttpException) {
              callback.get().onHttpError((ApolloHttpException) e);
            } else if (e instanceof ApolloNetworkException) {
              callback.get().onNetworkError((ApolloNetworkException) e);
            } else {
              callback.get().onFailure(e);
            }
          } finally {
            tracker.unregisterPrefetchCall(RealApolloPrefetch.this);
          }
        }
      }

      @Override public void onCompleted() {
        synchronized (RealApolloPrefetch.this) {
          callback = Optional.absent();
          completed = true;
        }
      }
    };
  }

  @Override public ApolloPrefetch clone() {
    return new RealApolloPrefetch(operation, serverUrl, httpCallFactory, httpCache, moshi, dispatcher, logger, tracker);
  }

  @Override public void cancel() {
    canceled = true;
    callback = Optional.absent();
    interceptorChain.dispose();
  }

  @Override public boolean isCanceled() {
    return canceled;
  }
}
