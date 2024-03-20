package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_8_3
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.http.valueOf
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.HttpCacheMissException
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeParseException

class CachingHttpInterceptor internal constructor(
    private val lruHttpCache: ApolloHttpCache
) : HttpInterceptor {

  constructor(
      directory: File,
      maxSize: Long,
      fileSystem: FileSystem = FileSystem.SYSTEM,
  ): this(DiskLruHttpCache(fileSystem, directory, maxSize))

  val cache: ApolloHttpCache = lruHttpCache

  @Suppress("DEPRECATION")
  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
    val policy = request.headers.valueOf(CACHE_FETCH_POLICY_HEADER) ?: defaultPolicy(request)
    val cacheKey = request.headers.valueOf(CACHE_KEY_HEADER)!!
    when (policy) {
      CACHE_FIRST -> {
        val cacheException: ApolloException
        try {
          return cacheMightThrow(request, cacheKey)
        } catch (e: ApolloException) {
          cacheException = e
        }

        try {
          return networkMightThrow(request, chain, cacheKey)
        } catch (e: ApolloException) {
          throw ApolloCompositeException(
              first = cacheException,
              second = e
          )
        }
      }
      CACHE_ONLY -> {
        return cacheMightThrow(request, cacheKey)
      }
      NETWORK_ONLY -> {
        return networkMightThrow(request, chain, cacheKey)
      }
      NETWORK_FIRST -> {
        val networkException: ApolloException
        try {
          val response = networkMightThrow(request, chain, cacheKey)
          if (response.statusCode in 200..299) {
            //  let HTTP errors through
            return response
          } else {
            throw ApolloHttpException(
                statusCode = response.statusCode,
                headers = response.headers,
                body = null,
                message = "Http request failed with status code `${response.statusCode}`"
            )
          }
        } catch (e: ApolloException) {
          // Original cause of network request failure
          networkException = e
        }

        try {
          return cacheMightThrow(request, cacheKey)
        } catch (cacheMissException: HttpCacheMissException) {
          // In case of exception thrown by network request,
          // ApolloException will be suppressed and HttpCacheMissException will throw as cause
          // this behavior might change in future where both will be treated as suppressed
          throw ApolloCompositeException(
              first = networkException,
              second = cacheMissException
          )
        }
      }
      else -> {
        error("Unknown HTTP fetch policy: $policy")
      }
    }
  }

  @Suppress("DEPRECATION")
  private fun defaultPolicy(request: HttpRequest): String {
    return if (request.headers.firstOrNull { it.name == CACHE_OPERATION_TYPE_HEADER }?.value == "query") {
      CACHE_FIRST
    } else {
      NETWORK_ONLY
    }
  }

  @Suppress("DEPRECATION")
  private suspend fun networkMightThrow(request: HttpRequest, chain: HttpInterceptorChain, cacheKey: String): HttpResponse {
    val response = chain.proceed(request)

    val doNotStore = request.headers.valueOf(CACHE_DO_NOT_STORE)?.lowercase() == "true"
    if (response.statusCode in 200..299 && !doNotStore) {
      // Note: this write may fail if the same cacheKey is being stored by another thread.
      // This is OK though: the other thread will be the one that stores it in the cache (see issue #3664).
      return lruHttpCache.write(
          response.newBuilder()
              .addHeaders(
                  listOf(
                      HttpHeader(CACHE_KEY_HEADER, cacheKey),
                      HttpHeader(CACHE_SERVED_DATE_HEADER, Instant.now().toString()),
                  )
              )
              .build(),
          cacheKey)
    }
    return response
  }

  @Suppress("DEPRECATION")
  private fun cacheMightThrow(request: HttpRequest, cacheKey: String): HttpResponse {
    val operationName = request.headers.valueOf(DefaultHttpRequestComposer.HEADER_APOLLO_OPERATION_NAME)
    val response = try {
      lruHttpCache.read(cacheKey)
          .newBuilder()
          .addHeaders(
              listOf(
                  HttpHeader(FROM_CACHE, "true"),
                  HttpHeader(CACHE_KEY_HEADER, cacheKey),
              )
          )
          .build()
    } catch (e: Exception) {
      throw HttpCacheMissException("HTTP Cache miss for $operationName", e)
    }

    val expireAfterRead = request.headers.valueOf(CACHE_EXPIRE_AFTER_READ_HEADER)?.lowercase() == "true"
    if (expireAfterRead) {
      try {
        lruHttpCache.remove(cacheKey)
      } catch (_: IOException) {
      }
    }

    val timeoutMillis = request.headers.valueOf(CACHE_EXPIRE_TIMEOUT_HEADER)?.toLongOrNull() ?: 0
    val servedDateMillis = try {
      Instant.parse(response.headers.valueOf(CACHE_SERVED_DATE_HEADER)).toEpochMilli()
    } catch (e: DateTimeParseException) {
      0L
    }
    val nowMillis = Instant.now().toEpochMilli()

    if (timeoutMillis > 0 && servedDateMillis > 0 && nowMillis - servedDateMillis > timeoutMillis) {
      // stale response
      throw HttpCacheMissException("HTTP Cache stale response for $operationName (served ${response.headers.valueOf(CACHE_SERVED_DATE_HEADER)})")
    }

    return response
  }

  @Deprecated("Use store.clearAll() instead", ReplaceWith("store.clearAll()"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_1_1)
  fun delete() {
    lruHttpCache.clearAll()
  }

  @Deprecated("Use store.remove(key) instead", ReplaceWith("store.remove(key)"))
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_1_1)
  fun remove(key: String) {
    lruHttpCache.remove(key)
  }

  companion object {

    fun cacheKey(httpRequest: HttpRequest): String {
      return when (httpRequest.method) {
        HttpMethod.Get -> ("Get" + httpRequest.url).toByteArray().toByteString().md5().hex()
        HttpMethod.Post -> {
          val buffer = Buffer()
          buffer.writeUtf8("Post")
          httpRequest.body!!.writeTo(buffer)
          buffer.md5().hex()
        }
      }
    }

    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_KEY_HEADER = "X-APOLLO-CACHE-KEY"

    internal const val REQUEST_UUID_HEADER = "X-APOLLO-REQUEST-UUID"

    /**
     * Cache fetch strategy http header
     */
    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_FETCH_POLICY_HEADER = "X-APOLLO-CACHE-FETCH-POLICY"

    /**
     * Cache operation type http header
     */
    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_OPERATION_TYPE_HEADER = "X-APOLLO-CACHE-OPERATION-TYPE"

    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_ONLY = "CACHE_ONLY"

    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val NETWORK_ONLY = "NETWORK_ONLY"

    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_FIRST = "CACHE_FIRST"

    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val NETWORK_FIRST = "NETWORK_FIRST"

    /**
     * Request served Date/time http header
     */
    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_SERVED_DATE_HEADER = "X-APOLLO-SERVED-DATE"

    /**
     * Cached response expiration timeout http header (in millisecond)
     */
    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_EXPIRE_TIMEOUT_HEADER = "X-APOLLO-EXPIRE-TIMEOUT"

    /**
     * Expire cached response flag http header
     */
    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_EXPIRE_AFTER_READ_HEADER = "X-APOLLO-EXPIRE-AFTER-READ"

    /**
     * Do not store the http response
     */
    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val CACHE_DO_NOT_STORE = "X-APOLLO-CACHE-DO-NOT-STORE"

    /**
     * Signals that HTTP response comes from the local cache
     */
    @Deprecated("This shouldn't be part of the public API and will be removed in Apollo Kotlin 4. If you needed this, please open an issue.")
    @ApolloDeprecatedSince(v3_8_3)
    const val FROM_CACHE = "X-APOLLO-FROM-CACHE"
  }
}
