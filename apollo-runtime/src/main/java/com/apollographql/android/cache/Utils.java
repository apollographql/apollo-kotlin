package com.apollographql.android.cache;

import java.io.IOException;
import java.util.Date;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.internal.Util;
import okhttp3.internal.http.HttpDate;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;

final class Utils {

  static Response strip(Response response) {
    return response != null && response.body() != null
        ? response.newBuilder().body(null).networkResponse(null).cacheResponse(null).build()
        : response;
  }

  static boolean isCacheEnable(Request request) {
    HttpCache.CacheControl cacheControl = cacheControl(request);
    return cacheControl == HttpCache.CacheControl.DEFAULT
        || cacheControl == HttpCache.CacheControl.NETWORK_BEFORE_STALE;
  }

  static HttpCache.CacheControl cacheControl(Request request) {
    return HttpCache.CacheControl.valueOfHttpHeader(request.header(HttpCache.CACHE_CONTROL_HEADER));
  }

  static Response withServedDateHeader(Response response) throws IOException {
    return response.newBuilder()
        .addHeader(HttpCache.CACHE_SERVED_DATE_HEADER, HttpDate.format(new Date()))
        .build();
  }

  static boolean isPrefetchResponse(Request request) {
    return Boolean.TRUE.toString().equals(request.header(HttpCache.CACHE_PREFETCH_HEADER));
  }

  static boolean shouldSkipCache(Request request) {
    HttpCache.CacheControl cacheControl = cacheControl(request);
    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    return cacheControl == null
        || cacheControl == HttpCache.CacheControl.NETWORK_ONLY
        || cacheKey == null;
  }

  static boolean shouldSkipNetwork(Request request) {
    HttpCache.CacheControl cacheControl = cacheControl(request);
    return cacheControl == HttpCache.CacheControl.CACHE_ONLY;
  }

  static Response unsatisfiableCacheRequest(Request request) {
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

  static void copyResponseBody(Response response, Sink sink) throws IOException {
    final int bufferSize = 8 * 1024;
    BufferedSource responseBodySource = response.body().source();
    BufferedSink cacheResponseBody = Okio.buffer(sink);
    while (responseBodySource.read(cacheResponseBody.buffer(), bufferSize) > 0) {
      cacheResponseBody.emit();
    }
    Util.closeQuietly(responseBodySource);
    cacheResponseBody.close();
  }

  private Utils() {
  }
}
