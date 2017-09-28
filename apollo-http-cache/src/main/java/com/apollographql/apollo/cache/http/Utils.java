package com.apollographql.apollo.cache.http;

import com.apollographql.apollo.api.cache.http.HttpCachePolicy;

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

import static com.apollographql.apollo.api.cache.http.HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER;
import static com.apollographql.apollo.api.cache.http.HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER;
import static com.apollographql.apollo.api.cache.http.HttpCache.CACHE_FETCH_STRATEGY_HEADER;
import static com.apollographql.apollo.api.cache.http.HttpCache.CACHE_KEY_HEADER;
import static com.apollographql.apollo.api.cache.http.HttpCache.CACHE_PREFETCH_HEADER;
import static com.apollographql.apollo.api.cache.http.HttpCache.CACHE_SERVED_DATE_HEADER;

final class Utils {
  static Response strip(Response response) {
    return response != null && response.body() != null
        ? response.newBuilder().body(null).networkResponse(null).cacheResponse(null).build()
        : response;
  }

  static Response withServedDateHeader(Response response) throws IOException {
    return response.newBuilder()
        .addHeader(CACHE_SERVED_DATE_HEADER, HttpDate.format(new Date()))
        .build();
  }

  static boolean isPrefetchResponse(Request request) {
    return Boolean.TRUE.toString().equalsIgnoreCase(request.header(CACHE_PREFETCH_HEADER));
  }

  static boolean shouldSkipCache(Request request) {
    String cacheKey = request.header(CACHE_KEY_HEADER);
    HttpCachePolicy.FetchStrategy fetchStrategy = fetchStrategy(request);
    return cacheKey == null
        || cacheKey.isEmpty()
        || fetchStrategy == null;
  }

  static boolean shouldSkipNetwork(Request request) {
    String cacheKey = request.header(CACHE_KEY_HEADER);
    HttpCachePolicy.FetchStrategy fetchStrategy = fetchStrategy(request);
    return cacheKey != null
        && !cacheKey.isEmpty()
        && fetchStrategy == HttpCachePolicy.FetchStrategy.CACHE_ONLY;
  }

  static boolean isNetworkOnly(Request request) {
    HttpCachePolicy.FetchStrategy fetchStrategy = fetchStrategy(request);
    return fetchStrategy == HttpCachePolicy.FetchStrategy.NETWORK_ONLY;
  }

  static boolean isNetworkFirst(Request request) {
    HttpCachePolicy.FetchStrategy fetchStrategy = fetchStrategy(request);
    return fetchStrategy == HttpCachePolicy.FetchStrategy.NETWORK_FIRST;
  }

//  static boolean shouldReturnStaleCache(Request request) {
//    String expireTimeoutHeader = request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER);
//    return expireTimeoutHeader == null || expireTimeoutHeader.isEmpty();
//  }

  static boolean shouldExpireAfterRead(Request request) {
    return Boolean.TRUE.toString().equalsIgnoreCase(request.header(CACHE_EXPIRE_AFTER_READ_HEADER));
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

  static void closeQuietly(Source source) {
    try {
      source.close();
    } catch (Exception ignore) {
      // ignore
    }
  }

  static void closeQuietly(Response response) {
    try {
      if (response != null) {
        response.close();
      }
    } catch (Exception ignore) {
      // ignore
    }
  }

  static boolean isStale(Request request, Response response) {
    String timeoutStr = request.header(CACHE_EXPIRE_TIMEOUT_HEADER);
    String servedDateStr = response.header(CACHE_SERVED_DATE_HEADER);
    if (servedDateStr == null || timeoutStr == null) {
      return true;
    }

    long timeout = Long.parseLong(timeoutStr);
    if (timeout == 0) {
      return false;
    }

    Date servedDate = HttpDate.parse(servedDateStr);
    long now = System.currentTimeMillis();
    return servedDate == null || now - servedDate.getTime() > timeout;
  }

  private static HttpCachePolicy.FetchStrategy fetchStrategy(Request request) {
    String fetchStrategyHeader = request.header(CACHE_FETCH_STRATEGY_HEADER);
    if (fetchStrategyHeader == null || fetchStrategyHeader.isEmpty()) {
      return null;
    }

    for (HttpCachePolicy.FetchStrategy fetchStrategy : HttpCachePolicy.FetchStrategy.values()) {
      if (fetchStrategy.name().equals(fetchStrategyHeader)) {
        return fetchStrategy;
      }
    }

    return null;
  }

  private Utils() {
  }
}
