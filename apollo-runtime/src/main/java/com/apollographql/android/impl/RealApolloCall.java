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
import com.apollographql.android.cache.normalized.CacheKeyResolver;
import com.apollographql.android.cache.normalized.Record;
import com.apollographql.android.cache.normalized.ResponseNormalizer;
import com.apollographql.android.impl.util.HttpException;
import com.apollographql.android.impl.util.Utils;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.internal.Util;

final class RealApolloCall<T extends Operation.Data> extends BaseApolloCall implements ApolloCall<T> {
  volatile Call httpCall;
  private final Cache cache;
  private CacheControl cacheControl;
  private final HttpCache httpCache;
  private HttpCacheControl httpCacheControl = HttpCacheControl.CACHE_FIRST;
  private final ResponseFieldMapper responseFieldMapper;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private boolean executed;

  RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache, Moshi moshi,
      ResponseFieldMapper responseFieldMapper, Map<ScalarType, CustomTypeAdapter> customTypeAdapters, Cache cache) {
    super(operation, serverUrl, httpCallFactory, moshi);
    this.httpCache = httpCache;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.cache = cache;
  }

  private RealApolloCall(Operation operation, HttpUrl serverUrl, Call.Factory httpCallFactory, HttpCache httpCache,
      HttpCacheControl httpCacheControl, Moshi moshi, ResponseFieldMapper responseFieldMapper,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, Cache cache, CacheControl cacheControl) {
    super(operation, serverUrl, httpCallFactory, moshi);
    this.httpCache = httpCache;
    this.httpCacheControl = httpCacheControl;
    this.responseFieldMapper = responseFieldMapper;
    this.customTypeAdapters = customTypeAdapters;
    this.cache = cache;
    this.cacheControl = cacheControl;
  }

  @Nonnull @Override public Response<T> execute() throws IOException {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    if (cacheControl == CacheControl.CACHE_ONLY || cacheControl == CacheControl.CACHE_FIRST) {
      Response<T> cachedResponse = cachedResponse();
      if (cachedResponse.data() != null || cacheControl == CacheControl.CACHE_ONLY) {
        return cachedResponse;
      }
    }

    return executeNetworkRequest();
  }

  @Nonnull @Override public ApolloCall<T> enqueue(@Nullable final Callback<T> callback) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
      executed = true;
    }

    //TODO must be called in own executor
    //issue: https://github.com/apollographql/apollo-android/issues/280
    if (cacheControl == CacheControl.CACHE_ONLY || cacheControl == CacheControl.CACHE_FIRST) {
      Response<T> cachedResponse = cachedResponse();
      if (cachedResponse.data() != null || cacheControl == CacheControl.CACHE_ONLY) {
        if (callback != null) {
          callback.onResponse(cachedResponse);
        }
        return this;
      }
    }

    try {
      httpCall = prepareHttpCall(httpCacheControl, false);
    } catch (Exception e) {
      if (callback != null) {
        callback.onFailure(e);
      }
      return this;
    }

    httpCall.enqueue(new okhttp3.Callback() {
      @Override public void onFailure(Call call, IOException e) {
        if (cacheControl == CacheControl.NETWORK_FIRST) {
          Response<T> cachedResponse = cachedResponse();
          if (cachedResponse.data() != null) {
            if (callback != null) {
              callback.onResponse(cachedResponse);
            }
            return;
          }
        }

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

  @Nonnull @Override public ApolloCall<T> httpCacheControl(@Nonnull HttpCacheControl httpCacheControl) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }

    Utils.checkNotNull(httpCacheControl, "httpCacheControl == null");
    this.httpCacheControl = httpCacheControl;
    return this;
  }

  @Nonnull @Override public ApolloCall<T> cacheControl(@Nonnull CacheControl cacheControl) {
    synchronized (this) {
      if (executed) throw new IllegalStateException("Already Executed");
    }

    Utils.checkNotNull(cacheControl, "cacheControl == null");
    this.cacheControl = cacheControl;
    return this;
  }

  @Override public void cancel() {
    Call call = httpCall;
    if (call != null) {
      call.cancel();
    }
  }

  @Override @Nonnull public ApolloCall<T> clone() {
    return new RealApolloCall<>(operation, serverUrl, httpCallFactory, httpCache, httpCacheControl, moshi,
        responseFieldMapper, customTypeAdapters, cache, cacheControl);
  }

  private Response<T> executeNetworkRequest() throws IOException {
    Response<T> networkResponse;
    try {
      httpCall = prepareHttpCall(httpCacheControl, false);
      networkResponse = handleResponse(httpCall.execute());
      if (!networkResponse.isSuccessful()) {
        Response<T> cachedResponse = cachedResponse();
        if (cachedResponse.data() != null && cachedResponse.isSuccessful()) {
          return cachedResponse;
        }
      }
      return networkResponse;
    } catch (Exception e) {
      if (cacheControl == CacheControl.NETWORK_FIRST) {
        Response<T> cachedResponse = cachedResponse();
        if (cachedResponse != null) {
          return cachedResponse;
        }
      }
      throw e;
    }
  }


  private Response<T> cachedResponse() {
    T cachedData = cachedData();
    if (cachedData != null) {
      return new Response<>(operation, cachedData, null);
    } else {
      return new Response<>(operation);
    }
  }

  @SuppressWarnings("unchecked") @Nullable private T cachedData() {
    Record rootRecord = cache.read(CacheKeyResolver.rootKeyForOperation(operation).key());
    if (rootRecord == null) {
      return null;
    }

    try {
      RealResponseReader<Record> responseReader = new RealResponseReader<>(operation, rootRecord,
          new CacheFieldValueResolver(cache, operation.variables()), customTypeAdapters);
      return (T) responseFieldMapper.map(responseReader);
    } catch (Exception e) {
      //TODO log me
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
