package com.apollographql.android.cache.http;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static com.apollographql.android.cache.http.Utils.cacheControl;
import static com.apollographql.android.cache.http.Utils.isCacheEnable;
import static com.apollographql.android.cache.http.Utils.isPrefetchResponse;
import static com.apollographql.android.cache.http.Utils.shouldExpireAfterRead;
import static com.apollographql.android.cache.http.Utils.shouldSkipCache;
import static com.apollographql.android.cache.http.Utils.shouldSkipNetwork;
import static com.apollographql.android.cache.http.Utils.strip;
import static com.apollographql.android.cache.http.Utils.unsatisfiableCacheRequest;
import static com.apollographql.android.cache.http.Utils.withServedDateHeader;

final class CacheInterceptor implements Interceptor {
  private final HttpCache cache;

  CacheInterceptor(HttpCache cache) {
    this.cache = cache;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if (shouldSkipCache(request)) {
      return chain.proceed(request);
    }

    if (shouldSkipNetwork(request)) {
      return cacheOnlyResponse(request);
    }

    if (!isCacheEnable(request)) {
      return chain.proceed(request);
    }

    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse == null) {
      Response networkResponse = withServedDateHeader(chain.proceed(request));
      if (isPrefetchResponse(request)) {
        return prefetch(networkResponse, cacheKey);
      } else {
        return cache.cacheProxy(networkResponse, cacheKey);
      }
    }

    if (!cache.isStale(cacheResponse)) {
      return cacheResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .request(request)
          .build();
    }

    Response networkResponse = withServedDateHeader(chain.proceed(request));
    return resolveResponse(networkResponse, cacheResponse, cacheKey, cacheControl(request),
        isPrefetchResponse(request));
  }

  private Response cacheOnlyResponse(Request request) throws IOException {
    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse != null && !cache.isStale(cacheResponse)) {
      return cacheResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .build();
    }
    return unsatisfiableCacheRequest(request);
  }

  private Response resolveResponse(Response networkResponse, Response cacheResponse, String cacheKey,
      HttpCache.CacheControl cacheControl, boolean prefetch) throws IOException {
    if (networkResponse.isSuccessful()) {
      cacheResponse.close();
      if (prefetch) {
        return prefetch(networkResponse, cacheKey);
      } else {
        return cache.cacheProxy(networkResponse, cacheKey)
            .newBuilder()
            .cacheResponse(strip(cacheResponse))
            .build();
      }
    }

    if (cacheControl == HttpCache.CacheControl.NETWORK_BEFORE_STALE) {
      return cacheResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .networkResponse(strip(networkResponse))
          .build();
    }

    cacheResponse.close();
    return networkResponse.newBuilder()
        .cacheResponse(strip(cacheResponse))
        .build();
  }

  private Response prefetch(Response networkResponse, String cacheKey) throws IOException {
    cache.write(networkResponse, cacheKey);
    Response cacheResponse = cache.read(cacheKey);
    if (cacheResponse == null) {
      throw new IOException("failed to read prefetch cache response");
    }
    return cacheResponse
        .newBuilder()
        .networkResponse(strip(networkResponse))
        .build();
  }
}
