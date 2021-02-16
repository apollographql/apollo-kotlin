package com.apollographql.apollo3.cache.http;

import com.apollographql.apollo3.api.cache.http.HttpCachePolicy;
import com.apollographql.apollo3.cache.http.internal.HttpDate;
import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;
import okio.Sink;
import okio.Source;

import static com.apollographql.apollo3.api.cache.http.HttpCache.CACHE_DO_NOT_STORE;
import static com.apollographql.apollo3.api.cache.http.HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER;
import static com.apollographql.apollo3.api.cache.http.HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER;
import static com.apollographql.apollo3.api.cache.http.HttpCache.CACHE_FETCH_STRATEGY_HEADER;
import static com.apollographql.apollo3.api.cache.http.HttpCache.CACHE_KEY_HEADER;
import static com.apollographql.apollo3.api.cache.http.HttpCache.CACHE_PREFETCH_HEADER;
import static com.apollographql.apollo3.api.cache.http.HttpCache.CACHE_SERVED_DATE_HEADER;

final class Utils {
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

  private static final ResponseBody EMPTY_RESPONSE = ResponseBody.create(null, EMPTY_BYTE_ARRAY);

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

  static boolean skipStoreResponse(Request request) {
    return Boolean.TRUE.toString().equalsIgnoreCase(request.header(CACHE_DO_NOT_STORE));
  }

  static Response unsatisfiableCacheRequest(Request request) {
    return new Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(504)
        .message("Unsatisfiable Request (cache-only)")
        .body(EMPTY_RESPONSE)
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

  static void closeQuietly(Closeable source) {
    try {
      source.close();
    } catch (Exception ignore) {
      // ignore
    }
  }

  /**
   * Attempts to exhaust {@code source}, returning true if successful. This is useful when reading a
   * complete source is helpful, such as when doing so completes a cache body or frees a socket
   * connection for reuse.
   *
   * <p>Copied from OkHttp.
   */
  static boolean discard(Source source, int timeout, TimeUnit timeUnit) {
    try {
      return skipAll(source, timeout, timeUnit);
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Reads until {@code in} is exhausted or the deadline has been reached. This is careful to not
   * extend the deadline if one exists already.
   *
   * <p>Copied from OkHttp.
   */
  static boolean skipAll(Source source, int duration, TimeUnit timeUnit) throws IOException {
    long now = System.nanoTime();
    long originalDuration = source.timeout().hasDeadline()
        ? source.timeout().deadlineNanoTime() - now
        : Long.MAX_VALUE;
    source.timeout().deadlineNanoTime(now + Math.min(originalDuration, timeUnit.toNanos(duration)));
    try {
      Buffer skipBuffer = new Buffer();
      while (source.read(skipBuffer, 8192) != -1) {
        skipBuffer.clear();
      }
      return true; // Success! The source has been exhausted.
    } catch (InterruptedIOException e) {
      return false; // We ran out of time before exhausting the source.
    } finally {
      if (originalDuration == Long.MAX_VALUE) {
        source.timeout().clearDeadline();
      } else {
        source.timeout().deadlineNanoTime(now + originalDuration);
      }
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

  /**
   * Returns the subset of the headers in {@code response}'s request that impact the content of
   * response's body.
   *
   * <p>Copied from OkHttp.
   */
  static Headers varyHeaders(Response response) {
    // Use the request headers sent over the network, since that's what the
    // response varies on. Otherwise OkHttp-supplied headers like
    // "Accept-Encoding: gzip" may be lost.
    Headers requestHeaders = response.networkResponse().request().headers();
    Headers responseHeaders = response.headers();
    return varyHeaders(requestHeaders, responseHeaders);
  }

  /**
   * Returns the subset of the headers in {@code requestHeaders} that impact the content of
   * response's body.
   *
   * <p>Copied from OkHttp.
   */
  static Headers varyHeaders(Headers requestHeaders, Headers responseHeaders) {
    Set<String> varyFields = varyFields(responseHeaders);
    if (varyFields.isEmpty()) return new Headers.Builder().build();

    Headers.Builder result = new Headers.Builder();
    for (int i = 0, size = requestHeaders.size(); i < size; i++) {
      String fieldName = requestHeaders.name(i);
      if (varyFields.contains(fieldName)) {
        result.add(fieldName, requestHeaders.value(i));
      }
    }
    return result.build();
  }

  /**
   * Returns the names of the request headers that need to be checked for equality when caching.
   *
   * <p>Copied from OkHttp.
   */
  static Set<String> varyFields(Headers responseHeaders) {
    Set<String> result = Collections.emptySet();
    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
      if (!"Vary".equalsIgnoreCase(responseHeaders.name(i))) continue;

      String value = responseHeaders.value(i);
      if (result.isEmpty()) {
        result = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
      }
      for (String varyField : value.split(",")) {
        result.add(varyField.trim());
      }
    }
    return result;
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
