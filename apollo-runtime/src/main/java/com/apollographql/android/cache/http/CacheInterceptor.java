package com.apollographql.android.cache.http;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static com.apollographql.android.cache.http.Utils.isNetworkFirst;
import static com.apollographql.android.cache.http.Utils.isPrefetchResponse;
import static com.apollographql.android.cache.http.Utils.shouldExpireAfterRead;
import static com.apollographql.android.cache.http.Utils.shouldReturnStaleCache;
import static com.apollographql.android.cache.http.Utils.shouldSkipCache;
import static com.apollographql.android.cache.http.Utils.shouldSkipNetwork;
import static com.apollographql.android.cache.http.Utils.strip;
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

    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    if (isNetworkFirst(request)) {
      return networkFirst(request, chain, cacheKey);
    } else {
      return cacheFirst(request, chain, cacheKey);
    }
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

  private Response networkFirst(Request request, Chain chain, String cacheKey) throws IOException {
    Response networkResponse = withServedDateHeader(chain.proceed(request));
    if (isPrefetchResponse(request)) {
      return prefetch(networkResponse, cacheKey);
    } else if (networkResponse.isSuccessful()) {
      return cache.cacheProxy(networkResponse, cacheKey);
    }

    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse == null) {
      return networkResponse;
    } else if (!cache.isStale(cacheResponse) || shouldReturnStaleCache(request)) {
      return cacheResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .networkResponse(strip(networkResponse))
          .request(request)
          .build();
    }

    return networkResponse.newBuilder()
        .cacheResponse(Utils.strip(cacheResponse))
        .build();
  }

  private Response cacheFirst(Request request, Chain chain, String cacheKey) throws IOException {
    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse == null || cache.isStale(cacheResponse)) {
      Response networkResponse = withServedDateHeader(chain.proceed(request));
      if (isPrefetchResponse(request)) {
        return prefetch(networkResponse, cacheKey);
      } else if (networkResponse.isSuccessful()) {
        return cache.cacheProxy(networkResponse, cacheKey);
      }
      return networkResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .request(request)
          .build();
    }

    return cacheResponse.newBuilder()
        .cacheResponse(strip(cacheResponse))
        .request(request)
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
