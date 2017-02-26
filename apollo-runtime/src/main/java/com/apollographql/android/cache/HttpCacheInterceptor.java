package com.apollographql.android.cache;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;

public final class HttpCacheInterceptor implements Interceptor {
  public static final String CACHE_KEY_HEADER = "APOLLO-CACHE-KEY";
  public static final String CACHE_CONTROL_HEADER = "APOLLO-CACHE-CONTROL";

  private final HttpCache cache;

  public HttpCacheInterceptor(HttpCache cache) {
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

    Response response = chain.proceed(request);
    if (isCacheEnable(request)) {
      String cacheKey = request.header(CACHE_KEY_HEADER);
      if (response.isSuccessful()) {
        return cache.cacheProxy(response, cacheKey);
      }
    }

    return response;
  }

  private boolean shouldSkipCache(Request request) {
    CacheControl cacheControl = cacheControl(request);
    String cacheKey = request.header(CACHE_KEY_HEADER);
    return cacheControl == null
        || cacheControl == CacheControl.NETWORK_ONLY
        || cacheKey == null;
  }

  private boolean shouldSkipNetwork(Request request) {
    CacheControl cacheControl = cacheControl(request);
    return cacheControl == CacheControl.CACHE_ONLY;
  }

  private Response cacheOnlyResponse(Request request) throws IOException {
    String cacheKey = request.header(CACHE_KEY_HEADER);
    Response cachedResponse = cache.read(cacheKey);
    if (cachedResponse != null) {
      return cachedResponse;
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
    return cacheControl == CacheControl.DEFAULT;
  }

  private CacheControl cacheControl(Request request) {
    return  CacheControl.valueOfHttpHeader(request.header(CACHE_CONTROL_HEADER));
  }

  public enum CacheControl {
    DEFAULT("default"),
    NETWORK_ONLY("network-only"),
    CACHE_ONLY("cache-only"),
    NETWORK_BEFORE_STALE("network-before-stale");

    public final String httpHeader;

    CacheControl(String httpHeader) {
      this.httpHeader = httpHeader;
    }

    static CacheControl valueOfHttpHeader(String header) {
      for (CacheControl value : values()) {
        if (value.httpHeader.equals(header)) {
          return value;
        }
      }
      return null;
    }
  }
}
