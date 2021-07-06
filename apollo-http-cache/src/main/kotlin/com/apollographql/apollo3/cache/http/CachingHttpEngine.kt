package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.api.http.withHeaders
import com.apollographql.apollo3.cache.http.internal.FileSystem
import com.apollographql.apollo3.exception.HttpCacheMissException
import com.apollographql.apollo3.network.http.DefaultHttpEngine
import com.apollographql.apollo3.network.http.HttpEngine
import okio.Buffer
import okio.ByteString.Companion.toByteString
import java.io.File
import java.time.Instant

class CachingHttpEngine(
    directory: File,
    maxSize: Long,
    fileSystem: FileSystem = FileSystem.SYSTEM,
    private val delegate: HttpEngine = DefaultHttpEngine()
) : HttpEngine {
  private val store = DiskLruHttpCache(fileSystem, directory, maxSize)

  override suspend fun execute(request: HttpRequest): HttpResponse {
    val policy = request.headers[CACHE_FETCH_POLICY_HEADER] ?: CACHE_FIRST
    val cacheKey = cacheKey(request)

    when (policy) {
      CACHE_FIRST -> {
        val responseResult = kotlin.runCatching {
          cacheMightThrow(request, cacheKey)
        }
        if (responseResult.isSuccess) {
          return responseResult.getOrThrow()
        }

        return networkMightThrow(request, cacheKey)
      }
      CACHE_ONLY -> {
        return cacheMightThrow(request, cacheKey)
      }
      NETWORK_ONLY -> {
        return networkMightThrow(request, cacheKey)
      }
      NETWORK_FIRST -> {
        val responseResult = kotlin.runCatching {
          networkMightThrow(request, cacheKey)
        }

        if (responseResult.isSuccess) {
          val response = responseResult.getOrThrow()
          if (response.statusCode in 200..299) {
            // special case, don't let HTTP errors through
            return response
          }
        }

        return cacheMightThrow(request, cacheKey)
      }
      else -> {
        error("Unknown HTTP fetch policy: $policy")
      }
    }
  }

  override fun dispose() {
  }

  private suspend fun networkMightThrow(request: HttpRequest, cacheKey: String): HttpResponse {
    val response = delegate.execute(request)

    val doNotStore = request.headers[CACHE_DO_NOT_STORE]?.lowercase() == "true"
    if (response.statusCode in 200..299 && !doNotStore) {
      store.write(
          response.withHeaders(
              mapOf(
                  CACHE_KEY_HEADER to cacheKey,
                  CACHE_SERVED_DATE_HEADER to Instant.now().toString(),
              )
          ),
          cacheKey)

      // Writing the response will consume the response body, so we re-read it from the cache
      return store.read(cacheKey)
    } else {
      return response
    }
  }

  private fun cacheMightThrow(request: HttpRequest, cacheKey: String): HttpResponse {
    val operationName = request.headers[DefaultHttpRequestComposer.HEADER_APOLLO_OPERATION_NAME]
    val response = try {
      store.read(cacheKey).withHeaders(
          mapOf(
              FROM_CACHE to "true",
              CACHE_KEY_HEADER to cacheKey,
          )
      )
    } catch (e: Exception) {
      throw HttpCacheMissException("HTTP Cache miss for $operationName", e)
    }

    val expireAfterRead = request.headers[CACHE_EXPIRE_AFTER_READ_HEADER]?.lowercase() == "true"
    if (expireAfterRead) {
      store.remove(cacheKey)
    }

    val timeoutMillis = request.headers[CACHE_EXPIRE_TIMEOUT_HEADER]?.toLongOrNull() ?: 0
    val servedDateMillis = kotlin.runCatching {
      Instant.parse(response.headers[CACHE_SERVED_DATE_HEADER]).toEpochMilli()
    }.recover { 0L }.getOrThrow()
    val nowMillis = Instant.now().toEpochMilli()

    if (timeoutMillis > 0 && servedDateMillis > 0 && nowMillis - servedDateMillis > timeoutMillis) {
      // stale response
      throw HttpCacheMissException("HTTP Cache stale response for $operationName (served ${response.headers[CACHE_SERVED_DATE_HEADER]})")
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