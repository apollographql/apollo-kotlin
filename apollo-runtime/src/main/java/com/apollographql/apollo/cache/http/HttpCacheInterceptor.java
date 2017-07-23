package com.apollographql.apollo.cache.http;

import com.apollographql.apollo.internal.util.ApolloLogger;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.cache.http.HttpCache.CACHE_KEY_HEADER;
import static com.apollographql.apollo.cache.http.Utils.isNetworkFirst;
import static com.apollographql.apollo.cache.http.Utils.isNetworkOnly;
import static com.apollographql.apollo.cache.http.Utils.isPrefetchResponse;
import static com.apollographql.apollo.cache.http.Utils.isStale;
import static com.apollographql.apollo.cache.http.Utils.shouldExpireAfterRead;
import static com.apollographql.apollo.cache.http.Utils.shouldSkipCache;
import static com.apollographql.apollo.cache.http.Utils.shouldSkipNetwork;
import static com.apollographql.apollo.cache.http.Utils.strip;
import static com.apollographql.apollo.cache.http.Utils.unsatisfiableCacheRequest;
import static com.apollographql.apollo.cache.http.Utils.withServedDateHeader;

public final class HttpCacheInterceptor implements Interceptor {
  private final HttpCache cache;
  private final ApolloLogger logger;

  HttpCacheInterceptor(HttpCache cache, ApolloLogger logger) {
    this.cache = checkNotNull(cache, "cache == null");
    this.logger = checkNotNull(logger, "logger == null");
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if (shouldSkipCache(request)) {
      logger.d("Skip http cache for request: %s", request);
      return chain.proceed(request);
    }

    if (shouldSkipNetwork(request)) {
      logger.d("Read http cache only for request: %s", request);
      return cacheOnlyResponse(request);
    }

    if (isNetworkOnly(request)) {
      logger.d("Skip http cache network only request: %s", request);
      return networkOnly(request, chain);
    }

    if (isNetworkFirst(request)) {
      logger.d("Network first for request: %s", request);
      return networkFirst(request, chain);
    } else {
      logger.d("Cache first for request: %s", request);
      return cacheFirst(request, chain);
    }
  }

  private Response cacheOnlyResponse(Request request) throws IOException {
    Response cacheResponse = cachedResponse(request);
    if (cacheResponse == null) {
      logCacheMiss(request);
      return unsatisfiableCacheRequest(request);
    }

    logCacheHit(request);
    return cacheResponse.newBuilder()
        .cacheResponse(strip(cacheResponse))
        .build();
  }

  private Response networkOnly(Request request, Chain chain) throws IOException {
    String cacheKey = request.header(CACHE_KEY_HEADER);
    Response networkResponse = withServedDateHeader(chain.proceed(request));
    if (isPrefetchResponse(request)) {
      return prefetch(networkResponse, cacheKey);
    } else if (networkResponse.isSuccessful()) {
      logger.d("Network success, skip http cache for request: %s, with cache key: %s", request, cacheKey);
      return cache.cacheProxy(networkResponse, cacheKey);
    } else {
      return networkResponse;
    }
  }

  private Response networkFirst(Request request, Chain chain) throws IOException {
    String cacheKey = request.header(CACHE_KEY_HEADER);
    IOException rethrowException;
    Response networkResponse = null;
    try {
      networkResponse = withServedDateHeader(chain.proceed(request));
      if (networkResponse.isSuccessful()) {
        logger.d("Network success, skip http cache for request: %s, with cache key: %s", request, cacheKey);
        return cache.cacheProxy(networkResponse, cacheKey);
      }
      rethrowException = null;
    } catch (IOException e) {
      rethrowException = e;
    }

    Response cachedResponse = cachedResponse(request);
    if (cachedResponse == null) {
      logCacheMiss(request);
      if (rethrowException != null) {
        throw rethrowException;
      }
      return networkResponse;
    }

    logCacheHit(request);
    return cachedResponse.newBuilder()
        .cacheResponse(strip(cachedResponse))
        .networkResponse(strip(networkResponse))
        .request(request)
        .build();
  }

  private Response cacheFirst(Request request, Chain chain) throws IOException {
    Response cachedResponse = cachedResponse(request);
    if (cachedResponse != null) {
      logCacheHit(request);
      return cachedResponse.newBuilder()
          .cacheResponse(strip(cachedResponse))
          .request(request)
          .build();
    }

    logCacheMiss(request);

    String cacheKey = request.header(CACHE_KEY_HEADER);
    Response networkResponse = withServedDateHeader(chain.proceed(request));
    if (isPrefetchResponse(request)) {
      return prefetch(networkResponse, cacheKey);
    } else if (networkResponse.isSuccessful()) {
      return cache.cacheProxy(networkResponse, cacheKey);
    }
    return networkResponse;
  }

  private Response prefetch(Response networkResponse, String cacheKey) throws IOException {
    if (!networkResponse.isSuccessful()) {
      return networkResponse;
    }

    try {
      cache.write(networkResponse, cacheKey);
    } finally {
      networkResponse.close();
    }

    Response cachedResponse = cache.read(cacheKey);
    if (cachedResponse == null) {
      throw new IOException("failed to read prefetch cache response");
    }

    return cachedResponse
        .newBuilder()
        .networkResponse(strip(networkResponse))
        .build();
  }

  private Response cachedResponse(Request request) {
    String cacheKey = request.header(CACHE_KEY_HEADER);

    Response cachedResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cachedResponse == null) {
      return null;
    }

    if (isStale(request, cachedResponse)) {
      Utils.closeQuietly(cachedResponse);
      return null;
    }

    return cachedResponse;
  }

  private void logCacheHit(Request request) {
    String cacheKey = request.header(CACHE_KEY_HEADER);
    logger.d("Cache HIT for request: %s, with cache key: %s", request, cacheKey);
  }

  private void logCacheMiss(Request request) {
    String cacheKey = request.header(CACHE_KEY_HEADER);
    logger.d("Cache MISS for request: %s, with cache key: %s", request, cacheKey);
  }
}
