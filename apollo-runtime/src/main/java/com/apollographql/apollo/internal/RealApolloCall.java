package com.apollographql.apollo.internal;

import com.apollographql.apollo.ApolloCall;
import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.cache.http.HttpCacheControl;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.interceptor.ApolloInterceptor;
import com.apollographql.apollo.interceptor.ApolloInterceptorChain;
import com.apollographql.apollo.internal.cache.http.HttpCache;
import com.apollographql.apollo.internal.cache.normalized.Cache;
import com.apollographql.apollo.internal.interceptor.ApolloCacheInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloParseInterceptor;
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor;
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain;
import com.apollographql.apollo.internal.util.ApolloLogger;
import com.squareup.moshi.Moshi;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

@SuppressWarnings("WeakerAccess") public final class RealApolloCall<T> implements ApolloCall<T> {
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
  final ApolloInterceptorChain interceptorChain;
  final ExecutorService dispatcher;
  final ApolloLogger logger;
  volatile boolean executed;

  public RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      HttpCacheControl httpCacheControl, Moshi moshi, ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, Cache cache, CacheControl cacheControl,
      ExecutorService dispatcher, ApolloLogger logger) {
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
    this.logger = logger;

    interceptorChain = new RealApolloInterceptorChain(operation, Arrays.asList(
        new ApolloCacheInterceptor(cache, cacheControl, responseFieldMapper, customTypeAdapters, dispatcher, logger),
        new ApolloParseInterceptor(httpCache, cache.networkResponseNormalizer(), responseFieldMapper,
            customTypeAdapters, logger),
        new ApolloServerInterceptor(serverUrl, httpCallFactory, httpCacheControl, false, moshi, logger)
    ));
  }

  @SuppressWarnings("unchecked") @Nonnull @Override public Response<T> execute() throws ApolloException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    return interceptorChain.proceed().parsedResponse.get();
  }

  @Override public void enqueue(@Nullable final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    interceptorChain.proceedAsync(dispatcher, new ApolloInterceptor.CallBack() {
      @Override public void onResponse(@Nonnull ApolloInterceptor.InterceptorResponse response) {
        if (callback != null) {
          //noinspection unchecked
          callback.onResponse(response.parsedResponse.get());
        }
      }

      @Override public void onFailure(@Nonnull ApolloException e) {
        if (callback != null) {
          if (e instanceof ApolloHttpException) {
            callback.onHttpError((ApolloHttpException) e);
          } else if (e instanceof ApolloParseException) {
            callback.onParseError((ApolloParseException) e);
          } else if (e instanceof ApolloNetworkException) {
            callback.onNetworkError((ApolloNetworkException) e);
          } else {
            callback.onFailure(e);
          }
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
        cache, cacheControl, dispatcher, logger);
  }

  @Nonnull @Override public RealApolloCall<T> cacheControl(@Nonnull CacheControl cacheControl) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCacheControl, moshi,
        responseFieldMapper, customTypeAdapters, cache, checkNotNull(cacheControl, "cacheControl == null"),
        dispatcher, logger);
  }

  @Override public void cancel() {
    interceptorChain.dispose();
  }

  @Override @Nonnull public RealApolloCall<T> clone() {
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCacheControl, moshi,
        responseFieldMapper, customTypeAdapters, cache, cacheControl, dispatcher, logger);
  }
}
