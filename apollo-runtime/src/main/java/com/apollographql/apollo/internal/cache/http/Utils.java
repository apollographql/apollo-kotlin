package com.apollographql.apollo.internal.cache.http;

import com.apollographql.apollo.cache.http.HttpCacheControl;

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
import okio.Source;

final class Utils {
  static Response strip(Response response) {
    return response != null && response.body() != null
        ? response.newBuilder().body(null).networkResponse(null).cacheResponse(null).build()
        : response;
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
    HttpCacheControl cacheControl = cacheControl(request);
    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    return cacheControl == null
        || cacheControl == HttpCacheControl.NETWORK_ONLY
        || cacheKey == null;
  }

  static boolean shouldSkipNetwork(Request request) {
    HttpCacheControl cacheControl = cacheControl(request);
    return cacheControl == HttpCacheControl.CACHE_ONLY;
  }

  static boolean isNetworkFirst(Request request) {
    HttpCacheControl cacheControl = cacheControl(request);
    return cacheControl == HttpCacheControl.NETWORK_FIRST
        || cacheControl == HttpCacheControl.NETWORK_BEFORE_STALE;
  }

  static boolean shouldReturnStaleCache(Request request) {
    HttpCacheControl cacheControl = cacheControl(request);
    return cacheControl == HttpCacheControl.NETWORK_BEFORE_STALE;
  }

  static boolean shouldExpireAfterRead(Request request) {
    HttpCacheControl cacheControl = cacheControl(request);
    return cacheControl == HttpCacheControl.EXPIRE_AFTER_READ;
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
    closeQuietly(responseBodySource);
  }

  private static void closeQuietly(Source source) {
    try {
      source.close();
    } catch (Exception ignore) {
      // ignore
    }
  }

  private static HttpCacheControl cacheControl(Request request) {
    return HttpCacheControl.valueOfHttpHeader(request.header(HttpCache.CACHE_CONTROL_HEADER));
  }

  private Utils() {
  }
}
