package com.apollographql.android.cache.http;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static com.apollographql.android.cache.http.Utils.shouldExpireAfterRead;

final class CacheInterceptor implements Interceptor {
  private final HttpCache cache;

  CacheInterceptor(HttpCache cache) {
    this.cache = cache;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if (Utils.shouldSkipCache(request)) {
      return chain.proceed(request);
    }

    if (Utils.shouldSkipNetwork(request)) {
      return cacheOnlyResponse(request);
    }

    if (!Utils.isCacheEnable(request)) {
      return chain.proceed(request);
    }

    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse == null) {
      Response networkResponse = Utils.withServedDateHeader(chain.proceed(request));
      if (Utils.isPrefetchResponse(request)) {
        return prefetch(networkResponse, cacheKey);
      } else {
        if (networkResponse.isSuccessful()) {
          return cache.cacheProxy(networkResponse, cacheKey);
        } else {
          return networkResponse;
        }
      }
    }

    if (!cache.isStale(cacheResponse)) {
      return cacheResponse.newBuilder()
          .cacheResponse(Utils.strip(cacheResponse))
          .request(request)
          .build();
    }

    Response networkResponse = Utils.withServedDateHeader(chain.proceed(request));
    return resolveResponse(networkResponse, cacheResponse, cacheKey, Utils.cacheControl(request),
        Utils.isPrefetchResponse(request));
  }

  private Response cacheOnlyResponse(Request request) throws IOException {
    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse != null && !cache.isStale(cacheResponse)) {
      return cacheResponse.newBuilder()
          .cacheResponse(Utils.strip(cacheResponse))
          .build();
    }
    return Utils.unsatisfiableCacheRequest(request);
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
            .cacheResponse(Utils.strip(cacheResponse))
            .build();
      }
    }

    if (cacheControl == HttpCache.CacheControl.NETWORK_BEFORE_STALE) {
      return cacheResponse.newBuilder()
          .cacheResponse(Utils.strip(cacheResponse))
          .networkResponse(Utils.strip(networkResponse))
          .build();
    }

    cacheResponse.close();
    return networkResponse.newBuilder()
        .cacheResponse(Utils.strip(cacheResponse))
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
        .networkResponse(Utils.strip(networkResponse))
        .build();
  }
}
