package com.apollographql.android.cache;

import java.io.IOException;
import java.util.Date;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.internal.http.HttpDate;

import static com.apollographql.android.cache.HttpCache.CacheControl;

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
    Response cacheResponse = cache.read(cacheKey);
    if (cacheResponse == null) {
      Response networkResponse = withServedDateHeader(chain.proceed(request));
      return cache.cacheProxy(networkResponse, cacheKey);
    }

    if (!cache.isStale(cacheResponse)) {
      return cacheResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .request(request)
          .build();
    }

    Response networkResponse = withServedDateHeader(chain.proceed(request));
    return resolveResponse(networkResponse, cacheResponse, cacheKey, cacheControl(request));
  }

  private boolean shouldSkipCache(Request request) {
    HttpCache.CacheControl cacheControl = cacheControl(request);
    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    return cacheControl == null
        || cacheControl == CacheControl.NETWORK_ONLY
        || cacheKey == null;
  }

  private boolean shouldSkipNetwork(Request request) {
    CacheControl cacheControl = cacheControl(request);
    return cacheControl == CacheControl.CACHE_ONLY;
  }

  private Response cacheOnlyResponse(Request request) throws IOException {
    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    Response cacheResponse = cache.read(cacheKey);
    if (cacheResponse != null && !cache.isStale(cacheResponse)) {
      return cacheResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .build();
    }
    return new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(504)
        .message("Unsatisfiable Request (cache-only)")
        .body(Util.EMPTY_RESPONSE)
        .sentRequestAtMillis(-1L)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build();
  }

  private boolean isCacheEnable(Request request) {
    CacheControl cacheControl = cacheControl(request);
    return cacheControl == CacheControl.DEFAULT || cacheControl == CacheControl.NETWORK_BEFORE_STALE;
  }

  private CacheControl cacheControl(Request request) {
    return CacheControl.valueOfHttpHeader(request.header(HttpCache.CACHE_CONTROL_HEADER));
  }

  private Response resolveResponse(Response networkResponse, Response cacheResponse, String cacheKey,
      CacheControl cacheControl) throws IOException {
    if (networkResponse.isSuccessful()) {
      cacheResponse.close();
      return cache.cacheProxy(networkResponse, cacheKey)
          .newBuilder()
          .cacheResponse(strip(cacheResponse))
          .build();
    }

    if (cacheControl == CacheControl.NETWORK_BEFORE_STALE) {
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

  private Response withServedDateHeader(Response response) throws IOException {
    return response.newBuilder()
        .addHeader(HttpCache.CACHE_SERVED_DATE_HEADER, HttpDate.format(new Date()))
        .build();
  }

  private static Response strip(Response response) {
    return response != null && response.body() != null
        ? response.newBuilder().body(null).networkResponse(null).cacheResponse(null).build()
        : response;
  }
}
