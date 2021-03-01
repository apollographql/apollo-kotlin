package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.Utils.__checkNotNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

internal class HttpCacheInterceptor(cache: ApolloHttpCache?, logger: ApolloLogger?) : Interceptor {
  private val cache: ApolloHttpCache
  private val logger: ApolloLogger
  @Throws(IOException::class)
  override fun intercept(chain: Interceptor.Chain): Response {
    val request = chain.request()
    if (Utils.shouldSkipCache(request)) {
      logger.d("Skip http cache for request: %s", request)
      return chain.proceed(request)
    }
    if (Utils.shouldSkipNetwork(request)) {
      logger.d("Read http cache only for request: %s", request)
      return cacheOnlyResponse(request)!!
    }
    if (Utils.isNetworkOnly(request)) {
      logger.d("Skip http cache network only request: %s", request)
      return networkOnly(request, chain)!!
    }
    return if (Utils.isNetworkFirst(request)) {
      logger.d("Network first for request: %s", request)
      networkFirst(request, chain)!!
    } else {
      logger.d("Cache first for request: %s", request)
      cacheFirst(request, chain)!!
    }
  }

  @Throws(IOException::class)
  private fun cacheOnlyResponse(request: Request): Response? {
    val cacheResponse = cachedResponse(request)
    if (cacheResponse == null) {
      logCacheMiss(request)
      return Utils.unsatisfiableCacheRequest(request)
    }
    logCacheHit(request)
    return cacheResponse.newBuilder()
        .cacheResponse(Utils.strip(cacheResponse))
        .build()
  }

  @Throws(IOException::class)
  private fun networkOnly(request: Request, chain: Interceptor.Chain): Response? {
    val cacheKey = request.header(HttpCache.CACHE_KEY_HEADER)
    val networkResponse = Utils.withServedDateHeader(chain.proceed(request))
    return if (Utils.isPrefetchResponse(request)) {
      prefetch(networkResponse, cacheKey)
    } else if (networkResponse!!.isSuccessful) {
      logger.d("Network success, skip http cache for request: %s, with cache key: %s", request, cacheKey!!)
      cache.cacheProxy(networkResponse, cacheKey)
    } else {
      networkResponse
    }
  }

  @Throws(IOException::class)
  private fun networkFirst(request: Request, chain: Interceptor.Chain): Response? {
    val cacheKey = request.header(HttpCache.CACHE_KEY_HEADER)
    var rethrowException: IOException?
    var networkResponse: Response? = null
    try {
      networkResponse = Utils.withServedDateHeader(chain.proceed(request))
      if (networkResponse.isSuccessful) {
        logger.d("Network success, skip http cache for request: %s, with cache key: %s", request, cacheKey!!)
        return cache.cacheProxy(networkResponse, cacheKey)
      }
      rethrowException = null
    } catch (e: IOException) {
      rethrowException = e
    }
    val cachedResponse = cachedResponse(request)
    if (cachedResponse == null) {
      logCacheMiss(request)
      if (rethrowException != null) {
        throw rethrowException
      }
      return networkResponse
    }
    logCacheHit(request)
    return cachedResponse.newBuilder()
        .cacheResponse(Utils.strip(cachedResponse))
        .networkResponse(Utils.strip(networkResponse))
        .request(request)
        .build()
  }

  @Throws(IOException::class)
  private fun cacheFirst(request: Request, chain: Interceptor.Chain): Response? {
    val cachedResponse = cachedResponse(request)
    if (cachedResponse != null) {
      logCacheHit(request)
      return cachedResponse.newBuilder()
          .cacheResponse(Utils.strip(cachedResponse))
          .request(request)
          .build()
    }
    logCacheMiss(request)
    val cacheKey = request.header(HttpCache.CACHE_KEY_HEADER)
    val networkResponse = Utils.withServedDateHeader(chain.proceed(request))
    if (Utils.isPrefetchResponse(request)) {
      return prefetch(networkResponse, cacheKey)
    } else if (networkResponse!!.isSuccessful) {
      return cache.cacheProxy(networkResponse, cacheKey!!)
    }
    return networkResponse
  }

  @Throws(IOException::class)
  private fun prefetch(networkResponse: Response?, cacheKey: String?): Response? {
    if (!networkResponse!!.isSuccessful) {
      return networkResponse
    }
    try {
      cache.write(networkResponse, cacheKey!!)
    } finally {
      networkResponse.close()
    }
    val cachedResponse = cache.read(cacheKey!!) ?: throw IOException("failed to read prefetch cache response")
    return cachedResponse
        .newBuilder()
        .networkResponse(Utils.strip(networkResponse))
        .build()
  }

  private fun cachedResponse(request: Request): Response? {
    val cacheKey = request.header(HttpCache.CACHE_KEY_HEADER)
    val cachedResponse = cache.read(cacheKey!!, Utils.shouldExpireAfterRead(request)) ?: return null
    if (Utils.isStale(request, cachedResponse)) {
      Utils.closeQuietly(cachedResponse)
      return null
    }
    return cachedResponse
  }

  private fun logCacheHit(request: Request) {
    val cacheKey = request.header(HttpCache.CACHE_KEY_HEADER)
    logger.d("Cache HIT for request: %s, with cache key: %s", request, cacheKey!!)
  }

  private fun logCacheMiss(request: Request) {
    val cacheKey = request.header(HttpCache.CACHE_KEY_HEADER)
    logger.d("Cache MISS for request: %s, with cache key: %s", request, cacheKey!!)
  }

  init {
    this.cache = __checkNotNull(cache, "cache == null")
    this.logger = __checkNotNull(logger, "logger == null")
  }
}