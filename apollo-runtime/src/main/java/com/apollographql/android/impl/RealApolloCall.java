package com.apollographql.android.impl;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.apollographql.android.cache.HttpCache;
import com.apollographql.android.impl.util.HttpException;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;

import static com.apollographql.android.cache.HttpCache.CacheControl;

final class RealApolloCall<T extends Operation.Data> extends BaseApolloCall implements ApolloCall<T> {
  volatile Call httpCall;
  private final HttpCache httpCache;
  private final ResponseBodyConverter responseBodyConverter;
  private boolean executed;
  private CacheControl cacheControl = CacheControl.DEFAULT;

  RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache, Moshi moshi,
      ResponseFieldMapper responseFieldMapper, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    super(operation, serverUrl, httpCallFactory, moshi);
    this.httpCache = httpCache;
    this.responseBodyConverter = new ResponseBodyConverter(operation, responseFieldMapper, customTypeAdapters);
  }

  private RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      CacheControl cacheControl, Moshi moshi, ResponseBodyConverter responseBodyConverter) {
    super(operation, serverUrl, httpCallFactory, moshi);
    this.httpCache = httpCache;
    this.cacheControl = cacheControl;
    this.responseBodyConverter = responseBodyConverter;
  }

  @Nonnull @Override public Response<T> execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }
    httpCall = prepareHttpCall(cacheControl, false);
    return handleResponse(httpCall.execute());
  }

  @Nonnull @Override public ApolloCall<T> enqueue(@Nullable final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    try {
      httpCall = prepareHttpCall(cacheControl, false);
    } catch (Exception e) {
      if (callback != null) {
        callback.onFailure(e);
      }
      return this;
    }

    httpCall.enqueue(new okhttp3.Callback() {
      @Override public void onFailure(Call call, IOException e) {
        if (callback != null) {
          callback.onFailure(e);
        }
      }

      @Override public void onResponse(Call call, okhttp3.Response httpResponse) throws IOException {
        try {
          Response<T> response = handleResponse(httpResponse);
          if (callback != null) {
            callback.onResponse(response);
          }
        } catch (Exception e) {
          if (callback != null) {
            callback.onFailure(e);
          }
        }
      }
    });
    return this;
  }

  @Nonnull @Override public ApolloCall<T> network() {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    cacheControl = CacheControl.NETWORK_ONLY;
    return this;
  }

  @Nonnull @Override public ApolloCall<T> cache() {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    cacheControl = CacheControl.CACHE_ONLY;
    return this;
  }

  @Nonnull @Override public ApolloCall<T> networkBeforeStale() {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    cacheControl = CacheControl.NETWORK_BEFORE_STALE;
    return this;
  }

  @Override public void cancel() {
    Call call = httpCall;
    if (call != null) {
      call.cancel();
    }
  }

  @Override @Nonnull public ApolloCall<T> clone() {
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, cacheControl, moshi,
        responseBodyConverter);
  }

  private <T extends Operation.Data> Response<T> handleResponse(okhttp3.Response response) throws IOException {
    int code = response.code();
    String cacheKey = response.request().header(HttpCache.CACHE_KEY_HEADER);
    if (code < 200 || code >= 300) {
      throw new HttpException(response);
    } else {
      try {
        return responseBodyConverter.convert(response.body());
      } catch (Exception rethrown) {
        httpCache.removeQuietly(cacheKey);
        throw rethrown;
      }
    }
  }
}
