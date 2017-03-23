package com.apollographql.android.impl;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.apollographql.android.cache.http.HttpCache;
import com.apollographql.android.cache.http.HttpCacheControl;
import com.apollographql.android.cache.normalized.Cache;
import com.apollographql.android.cache.normalized.CacheControl;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;

@SuppressWarnings("WeakerAccess") final class RealApolloCall<T> implements ApolloCall<T> {
  final Operation operation;
  final HttpUrl serverUrl;
  final Call.Factory httpCallFactory;
  final HttpCache httpCache;
  final HttpCacheControl httpCacheControl;
  final Moshi moshi;
  final ResponseFieldMapper responseFieldMapper;
  final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  final Cache cache;
  final CacheControl cacheControl;
  final CallInterceptorChain interceptorChain;
  final ExecutorService dispatcher;
  volatile boolean executed;

  RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      HttpCacheControl httpCacheControl, Moshi moshi, ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, Cache cache, CacheControl cacheControl,
      ExecutorService dispatcher) {
    this.operation = operation;
    this.serverUrl = serverUrl;
    this.httpCallFactory = httpCallFactory;
    this.httpCache = httpCache;
    this.httpCacheControl = httpCacheControl;
    this.moshi = moshi;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.cache = cache;
    this.cacheControl = cacheControl;
    this.dispatcher = dispatcher;

    interceptorChain = new RealCallInterceptorChain(operation)
        .chain(new CacheCallInterceptor(cache, cacheControl, responseFieldMapper, customTypeAdapters, dispatcher))
        .chain(new ParseCallInterceptor(httpCache, cache.networkResponseNormalizer(), responseFieldMapper,
            customTypeAdapters))
        .chain(new ServerCallInterceptor(serverUrl, httpCallFactory, httpCacheControl, false, moshi));
  }

  @SuppressWarnings("unchecked") @Nonnull @Override public Response<T> execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    return interceptorChain.proceed().parsedResponse;
  }

  @Override public void enqueue(@Nullable final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    interceptorChain.proceedAsync(dispatcher, new CallInterceptor.CallBack() {
      @SuppressWarnings("unchecked") @Override public void onResponse(CallInterceptor.InterceptorResponse response) {
        if (callback != null) {
          callback.onResponse(response.parsedResponse);
        }
      }

      @Override public void onFailure(Throwable t) {
        if (callback != null) {
          callback.onFailure(t);
        }
      }
    });
  }

  @Nonnull @Override public RealApolloWatcher<T> watcher() {
    return new RealApolloWatcher<>(clone(), cache);
  }

  @Nonnull @Override public RealApolloCall<T> httpCacheControl(@Nonnull HttpCacheControl httpCacheControl) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache,
        checkNotNull(httpCacheControl, "httpCacheControl == null"), moshi, responseFieldMapper, customTypeAdapters,
        cache, cacheControl, dispatcher);
  }

  @Nonnull @Override public RealApolloCall<T> cacheControl(@Nonnull CacheControl cacheControl) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCacheControl, moshi,
        responseFieldMapper, customTypeAdapters, cache, checkNotNull(cacheControl, "cacheControl == null"), dispatcher);
  }

  @Override public void cancel() {
    interceptorChain.dispose();
  }

  @Override @Nonnull public RealApolloCall<T> clone() {
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCacheControl, moshi,
        responseFieldMapper, customTypeAdapters, cache, cacheControl, dispatcher);
  }
}
