package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.http.valueOf
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.HttpCacheMissException
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import okio.Buffer
import okio.ByteString.Companion.toByteString
import okio.FileSystem
import java.io.File
import java.time.Instant
import java.time.format.DateTimeParseException

class CachingHttpInterceptor(
    directory: File,
    maxSize: Long,
    fileSystem: FileSystem = FileSystem.SYSTEM,
) : HttpInterceptor {
  private val store = DiskLruHttpCache(fileSystem, directory, maxSize)

  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
    val policy = request.headers.valueOf(CACHE_FETCH_POLICY_HEADER) ?: CACHE_FIRST
    val cacheKey = cacheKey(request)

    when (policy) {
      CACHE_FIRST -> {
        try {
          return cacheMightThrow(request, cacheKey)
        } catch (e: ApolloException) {
          //
        }

        return networkMightThrow(request, chain, cacheKey)
      }
      CACHE_ONLY -> {
        return cacheMightThrow(request, cacheKey)
      }
      NETWORK_ONLY -> {
        return networkMightThrow(request, chain, cacheKey)
      }
      NETWORK_FIRST -> {
        try {
          val response = networkMightThrow(request, chain, cacheKey)
          if (response.statusCode in 200..299) {
            //  let HTTP errors through
            return response
          }
        } catch (e: ApolloException) {

        }

        return cacheMightThrow(request, cacheKey)
      }
      else -> {
        error("Unknown HTTP fetch policy: $policy")
      }
    }
  }

  private suspend fun networkMightThrow(request: HttpRequest, chain: HttpInterceptorChain, cacheKey: String): HttpResponse {
    val response = chain.proceed(request)

    val doNotStore = request.headers.valueOf(CACHE_DO_NOT_STORE)?.lowercase() == "true"
    if (response.statusCode in 200..299 && !doNotStore) {
      // Note: this write may fail if the same cacheKey is being stored by another thread.
      // This is OK though: the other thread will be the one that stores it in the cache (see issue #3664).
      store.write(
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

  private fun cacheMightThrow(request: HttpRequest, cacheKey: String): HttpResponse {
    val operationName = request.headers.valueOf(DefaultHttpRequestComposer.HEADER_APOLLO_OPERATION_NAME)
    val response = try {
      store.read(cacheKey)
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
      store.remove(cacheKey)
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

  fun delete() {
    store.delete()
  }

  fun remove(key: String) {
    store.remove(key)
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

    /**
     *
     */
    const val CACHE_KEY_HEADER = "X-APOLLO-CACHE-KEY"

    /**
     * Cache fetch strategy http header
     */
    const val CACHE_FETCH_POLICY_HEADER = "X-APOLLO-CACHE-FETCH-POLICY"

    const val CACHE_ONLY = "CACHE_ONLY"
    const val NETWORK_ONLY = "NETWORK_ONLY"
    const val CACHE_FIRST = "CACHE_FIRST"
    const val NETWORK_FIRST = "NETWORK_FIRST"

    /**
     * Request served Date/time http header
     */
    const val CACHE_SERVED_DATE_HEADER = "X-APOLLO-SERVED-DATE"

    /**
     * Cached response expiration timeout http header (in millisecond)
     */
    const val CACHE_EXPIRE_TIMEOUT_HEADER = "X-APOLLO-EXPIRE-TIMEOUT"

    /**
     * Expire cached response flag http header
     */
    const val CACHE_EXPIRE_AFTER_READ_HEADER = "X-APOLLO-EXPIRE-AFTER-READ"

    /**
     * Do not store the http response
     */
    const val CACHE_DO_NOT_STORE = "X-APOLLO-CACHE-DO-NOT-STORE"

    /**
     * Signals that HTTP response comes from the local cache
     */
    const val FROM_CACHE = "X-APOLLO-FROM-CACHE"
  }
}
