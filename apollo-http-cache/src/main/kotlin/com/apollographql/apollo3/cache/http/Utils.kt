package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy.FetchStrategy
import com.apollographql.apollo3.cache.http.internal.HttpDate
import okhttp3.Headers
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.Sink
import okio.Source
import okio.buffer
import java.io.Closeable
import java.io.IOException
import java.io.InterruptedIOException
import java.util.Date
import java.util.TreeSet
import java.util.concurrent.TimeUnit

internal object Utils {
  private val EMPTY_BYTE_ARRAY = ByteArray(0)
  private val EMPTY_RESPONSE = ResponseBody.create(null, EMPTY_BYTE_ARRAY)
  fun strip(response: Response?): Response? {
    return response
        ?.newBuilder()
        ?.body(null)
        ?.networkResponse(null)
        ?.cacheResponse(null)
        ?.build()
  }

  @Throws(IOException::class)
  fun withServedDateHeader(response: Response): Response {
    return response.newBuilder()
        .addHeader(HttpCache.CACHE_SERVED_DATE_HEADER, HttpDate.format(Date()))
        .build()
  }

  fun isPrefetchResponse(request: Request): Boolean {
    return java.lang.Boolean.TRUE.toString().equals(request.header(HttpCache.CACHE_PREFETCH_HEADER), ignoreCase = true)
  }

  fun shouldSkipCache(request: Request): Boolean {
    val cacheKey = request.header(HttpCache.CACHE_KEY_HEADER)
    val fetchStrategy = fetchStrategy(request)
    return (cacheKey == null || cacheKey.isEmpty()
        || fetchStrategy == null)
  }

  fun shouldSkipNetwork(request: Request): Boolean {
    val cacheKey = request.header(HttpCache.CACHE_KEY_HEADER)
    val fetchStrategy = fetchStrategy(request)
    return (cacheKey != null && cacheKey.isNotEmpty()
        && fetchStrategy === FetchStrategy.CACHE_ONLY)
  }

  fun isNetworkOnly(request: Request): Boolean {
    val fetchStrategy = fetchStrategy(request)
    return fetchStrategy === FetchStrategy.NETWORK_ONLY
  }

  fun isNetworkFirst(request: Request): Boolean {
    val fetchStrategy = fetchStrategy(request)
    return fetchStrategy === FetchStrategy.NETWORK_FIRST
  }

  //  static boolean shouldReturnStaleCache(Request request) {
  //    String expireTimeoutHeader = request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER);
  //    return expireTimeoutHeader == null || expireTimeoutHeader.isEmpty();
  //  }
  fun shouldExpireAfterRead(request: Request): Boolean {
    return java.lang.Boolean.TRUE.toString().equals(request.header(HttpCache.CACHE_EXPIRE_AFTER_READ_HEADER), ignoreCase = true)
  }

  fun skipStoreResponse(request: Request): Boolean {
    return java.lang.Boolean.TRUE.toString().equals(request.header(HttpCache.CACHE_DO_NOT_STORE), ignoreCase = true)
  }

  fun unsatisfiableCacheRequest(request: Request?): Response {
    return Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(504)
        .message("Unsatisfiable Request (cache-only)")
        .body(EMPTY_RESPONSE)
        .sentRequestAtMillis(-1L)
        .receivedResponseAtMillis(System.currentTimeMillis())
        .build()
  }

  @Throws(IOException::class)
  fun copyResponseBody(response: Response, sink: Sink) {
    val bufferSize = 8 * 1024
    val responseBodySource = response.body()!!.source()
    val cacheResponseBody = sink.buffer()
    while (responseBodySource.read(cacheResponseBody.buffer, bufferSize.toLong()) > 0) {
      cacheResponseBody.emit()
    }
    closeQuietly(responseBodySource)
  }

  fun closeQuietly(source: Closeable) {
    try {
      source.close()
    } catch (ignore: Exception) {
      // ignore
    }
  }

  /**
   * Attempts to exhaust `source`, returning true if successful. This is useful when reading a
   * complete source is helpful, such as when doing so completes a cache body or frees a socket
   * connection for reuse.
   *
   *
   * Copied from OkHttp.
   */
  fun discard(source: Source, timeout: Int, timeUnit: TimeUnit): Boolean {
    return try {
      skipAll(source, timeout, timeUnit)
    } catch (e: IOException) {
      false
    }
  }

  /**
   * Reads until `in` is exhausted or the deadline has been reached. This is careful to not
   * extend the deadline if one exists already.
   *
   *
   * Copied from OkHttp.
   */
  @Throws(IOException::class)
  fun skipAll(source: Source, duration: Int, timeUnit: TimeUnit): Boolean {
    val now = System.nanoTime()
    val originalDuration = if (source.timeout().hasDeadline()) source.timeout().deadlineNanoTime() - now else Long.MAX_VALUE
    source.timeout().deadlineNanoTime(now + originalDuration.coerceAtMost(timeUnit.toNanos(duration.toLong())))
    return try {
      val skipBuffer = Buffer()
      while (source.read(skipBuffer, 8192) != -1L) {
        skipBuffer.clear()
      }
      true // Success! The source has been exhausted.
    } catch (e: InterruptedIOException) {
      false // We ran out of time before exhausting the source.
    } finally {
      if (originalDuration == Long.MAX_VALUE) {
        source.timeout().clearDeadline()
      } else {
        source.timeout().deadlineNanoTime(now + originalDuration)
      }
    }
  }

  fun isStale(request: Request, response: Response): Boolean {
    val timeoutStr = request.header(HttpCache.CACHE_EXPIRE_TIMEOUT_HEADER)
    val servedDateStr = response.header(HttpCache.CACHE_SERVED_DATE_HEADER)
    if (servedDateStr == null || timeoutStr == null) {
      return true
    }
    val timeout = timeoutStr.toLong()
    if (timeout == 0L) {
      return false
    }
    val servedDate = HttpDate.parse(servedDateStr)
    val now = System.currentTimeMillis()
    return servedDate == null || now - servedDate.time > timeout
  }

  /**
   * Returns the subset of the headers in `response`'s request that impact the content of
   * response's body.
   *
   *
   * Copied from OkHttp.
   */
  fun varyHeaders(response: Response): Headers {
    // Use the request headers sent over the network, since that's what the
    // response varies on. Otherwise OkHttp-supplied headers like
    // "Accept-Encoding: gzip" may be lost.
    val requestHeaders = response.networkResponse()!!.request().headers()
    val responseHeaders = response.headers()
    return varyHeaders(requestHeaders, responseHeaders)
  }

  /**
   * Returns the subset of the headers in `requestHeaders` that impact the content of
   * response's body.
   *
   *
   * Copied from OkHttp.
   */
  private fun varyHeaders(requestHeaders: Headers, responseHeaders: Headers): Headers {
    val varyFields = varyFields(responseHeaders)
    if (varyFields.isEmpty()) return Headers.Builder().build()
    val result = Headers.Builder()
    var i = 0
    val size = requestHeaders.size()
    while (i < size) {
      val fieldName = requestHeaders.name(i)
      if (varyFields.contains(fieldName)) {
        result.add(fieldName, requestHeaders.value(i))
      }
      i++
    }
    return result.build()
  }

  /**
   * Returns the names of the request headers that need to be checked for equality when caching.
   *
   *
   * Copied from OkHttp.
   */
  private fun varyFields(responseHeaders: Headers): Set<String> {
    var result = mutableSetOf<String>()
    var i = 0
    val size = responseHeaders.size()
    while (i < size) {
      if (!"Vary".equals(responseHeaders.name(i), ignoreCase = true)) {
        i++
        continue
      }
      val value = responseHeaders.value(i)
      if (result.isEmpty()) {
        result = TreeSet(java.lang.String.CASE_INSENSITIVE_ORDER)
      }
      for (varyField in value.split(",").toTypedArray()) {
        result.add(varyField.trim { it <= ' ' })
      }
      i++
    }
    return result
  }

  private fun fetchStrategy(request: Request): FetchStrategy? {
    val fetchStrategyHeader = request.header(HttpCache.CACHE_FETCH_STRATEGY_HEADER)
    if (fetchStrategyHeader == null || fetchStrategyHeader.isEmpty()) {
      return null
    }
    for (fetchStrategy in FetchStrategy.values()) {
      if (fetchStrategy.name == fetchStrategyHeader) {
        return fetchStrategy
      }
    }
    return null
  }
}