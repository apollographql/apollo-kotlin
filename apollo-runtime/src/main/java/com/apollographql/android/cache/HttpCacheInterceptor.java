package com.apollographql.android.cache;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class HttpCacheInterceptor implements Interceptor {
  public static final String CACHE_CONTROL_HEADER = "APOLLO-CACHE-CONTROL";

  private final HttpCache cache;

  public HttpCacheInterceptor(HttpCache cache) {
    this.cache = cache;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    Response response = chain.proceed(request);
    if (request.header(CACHE_CONTROL_HEADER) == null) {
      return response;
    } else {
      return cache.cacheProxy(response);
    }
  }
}
