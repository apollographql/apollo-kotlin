package com.apollographql.android.impl;

import com.apollographql.android.ApolloCall;
import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.apollographql.android.cache.http.HttpCache;
import com.apollographql.android.cache.normalized.Cache;
import com.apollographql.android.cache.normalized.CacheKeyResolver;
import com.apollographql.android.cache.normalized.Record;
import com.apollographql.android.cache.normalized.ResponseNormalizer;
import com.apollographql.android.impl.util.HttpException;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.internal.Util;

import static com.apollographql.android.cache.http.HttpCache.CacheControl;

final class RealApolloCall<T extends Operation.Data> extends BaseApolloCall implements ApolloCall<T> {
  volatile Call httpCall;
  private final Cache cache;
  private final HttpCache httpCache;
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private boolean executed;
  private CacheControl cacheControl = CacheControl.DEFAULT;

  RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache, Moshi moshi,
      ResponseFieldMapper responseFieldMapper, Map<ScalarType, CustomTypeAdapter> customTypeAdapters, Cache cache) {
    super(operation, serverUrl, httpCallFactory, moshi);
    this.httpCache = httpCache;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.cache = cache;
  }

  private RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      CacheControl cacheControl, Moshi moshi, ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, Cache cache) {
    super(operation, serverUrl, httpCallFactory, moshi);
    this.httpCache = httpCache;
    this.cacheControl = cacheControl;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.cache = cache;
  }

  @Nonnull @Override public Response<T> execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    Response<T> cachedResponse = cachedResponse();
    if (cachedResponse != null) {
      return cachedResponse;
    }

    httpCall = prepareHttpCall(cacheControl, false);
    return handleResponse(httpCall.execute());
  }

  @Nonnull @Override public ApolloCall<T> enqueue(@Nullable final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    //TODO must be called in own executor
    //issue: https://github.com/apollographql/apollo-android/issues/280
    Response<T> cachedResponse = cachedResponse();
    if (cachedResponse != null) {
      if (callback != null) {
        callback.onResponse(cachedResponse);
      }
      return this;
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

  @Nonnull @Override public ApolloCall<T> expireAfterRead() {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }
    cacheControl = CacheControl.EXPIRE_AFTER_READ;
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
        responseFieldMapper, customTypeAdapters, cache);
  }

  private Response<T> cachedResponse() {
    if (cacheControl == CacheControl.NETWORK_ONLY) {
      return null;
    }

    T cachedData = cachedData();
    if (cachedData != null) {
      return new Response<>(operation, cachedData, null);
    }

    return null;
  }

  @SuppressWarnings("unchecked") @Nullable private T cachedData() {
    Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation));
    if (rootRecord == null) {
      return null;
    }

    try {
      RealResponseReader<Record> responseReader = new RealResponseReader<>(operation, rootRecord,
          new CacheFieldValueResolver(cache, operation.variables()), customTypeAdapters);
      return (T) responseFieldMapper.map(responseReader);
    } catch (Exception e) {
      //TODO log me
      e.printStackTrace();
      return null;
    }
  }

  private <T extends Operation.Data> Response<T> handleResponse(okhttp3.Response response) throws IOException {
    String cacheKey = response.request().header(HttpCache.CACHE_KEY_HEADER);
    if (response.isSuccessful()) {
      try {
        ResponseNormalizer normalizer = cache.responseNormalizer();
        HttpResponseBodyConverter converter = new HttpResponseBodyConverter(operation, responseFieldMapper,
            customTypeAdapters);
        Response<T> convertedResponse = converter.convert(response.body(), normalizer);
        cache.write(normalizer.records());
        return convertedResponse;
      } catch (Exception rethrown) {
        Util.closeQuietly(response);
        if (httpCache != null) {
          httpCache.removeQuietly(cacheKey);
        }
        throw rethrown;
      }
    } else {
      Util.closeQuietly(response);
      throw new HttpException(response);
    }
  }
}
