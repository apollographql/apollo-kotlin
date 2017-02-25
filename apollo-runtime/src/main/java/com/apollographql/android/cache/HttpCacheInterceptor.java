package com.apollographql.android.cache;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;

public final class HttpCacheInterceptor implements Interceptor {
  public static final String CACHE_CONTROL_DEFAULT = "default";
  public static final String CACHE_CONTROL_NETWORK_ONLY = "network-only";
  public static final String CACHE_CONTROL_CACHE_ONLY = "cache-only";
  public static final String CACHE_CONTROL_NETWORK_BEFORE_STALE = "network-before-stale";

  public static final String CACHE_KEY_HEADER = "APOLLO-CACHE-KEY";
  public static final String CACHE_CONTROL_HEADER = "APOLLO-CACHE-CONTROL";

  private final HttpCache cache;

  public HttpCacheInterceptor(HttpCache cache) {
    this.cache = cache;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();

    String cacheControl = request.header(CACHE_CONTROL_HEADER);
    String cacheKey = request.header(CACHE_KEY_HEADER);
    if (cacheControl == null
        || CACHE_CONTROL_NETWORK_ONLY.equals(cacheControl)
        || cacheKey == null) {
      return chain.proceed(request);
    }

    if (CACHE_CONTROL_CACHE_ONLY.equals(cacheControl)) {
      Response cachedResponse = cache.read(cacheKey);
      if (cachedResponse != null) {
        return cachedResponse;
      }

      return new Response.Builder()
          .request(chain.request())
          .protocol(Protocol.HTTP_1_1)
          .code(504)
          .message("Unsatisfiable Request (cache-only)")
          .body(Util.EMPTY_RESPONSE)
          .sentRequestAtMillis(-1L)
          .receivedResponseAtMillis(System.currentTimeMillis())
          .build();
    }

    Response response = chain.proceed(request);
    if (response.isSuccessful() && CACHE_CONTROL_DEFAULT.equals(cacheControl)) {
      return cache.cacheProxy(response, cacheKey);
    } else {
      return response;
    }
  }
}
