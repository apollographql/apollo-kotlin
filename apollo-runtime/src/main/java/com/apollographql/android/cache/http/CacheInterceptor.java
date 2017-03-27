package com.apollographql.android.cache.http;

import com.apollographql.android.internal.ApolloLogger;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static com.apollographql.android.api.graphql.util.Utils.checkNotNull;
import static com.apollographql.android.cache.http.Utils.isNetworkFirst;
import static com.apollographql.android.cache.http.Utils.isPrefetchResponse;
import static com.apollographql.android.cache.http.Utils.shouldExpireAfterRead;
import static com.apollographql.android.cache.http.Utils.shouldReturnStaleCache;
import static com.apollographql.android.cache.http.Utils.shouldSkipCache;
import static com.apollographql.android.cache.http.Utils.shouldSkipNetwork;
import static com.apollographql.android.cache.http.Utils.strip;
import static com.apollographql.android.cache.http.Utils.withServedDateHeader;

final class CacheInterceptor implements Interceptor {
  private final HttpCache cache;
  private final ApolloLogger logger;

  CacheInterceptor(HttpCache cache, ApolloLogger logger) {
    this.cache = checkNotNull(cache, "cache == null");
    this.logger = checkNotNull(logger, "logger == null");
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    if (shouldSkipCache(request)) {
      logger.d("Skip cache for request: %s", request);
      return chain.proceed(request);
    }

    if (shouldSkipNetwork(request)) {
      logger.d("Cache only for request: %s", request);
      return cacheOnlyResponse(request);
    }

    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    if (isNetworkFirst(request)) {
      logger.d("Network first for request: %s", request);
      return networkFirst(request, chain, cacheKey);
    } else {
      logger.d("Cache first for request: %s", request);
      return cacheFirst(request, chain, cacheKey);
    }
  }

  private Response cacheOnlyResponse(Request request) throws IOException {
    String cacheKey = request.header(HttpCache.CACHE_KEY_HEADER);
    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse != null && !cache.isStale(cacheResponse)) {
      logCacheHit(request, cacheKey);
      return cacheResponse.newBuilder()
          .cacheResponse(Utils.strip(cacheResponse))
          .build();
    }
    logCacheMiss(request, cacheKey);
    return Utils.unsatisfiableCacheRequest(request);
  }

  private Response networkFirst(Request request, Chain chain, String cacheKey) throws IOException {
    Response networkResponse = withServedDateHeader(chain.proceed(request));
    if (isPrefetchResponse(request)) {
      return prefetch(networkResponse, cacheKey);
    } else if (networkResponse.isSuccessful()) {
      logger.d("Network success, skip cache for request: %s, with cache key: %s", request, cacheKey);
      return cache.cacheProxy(networkResponse, cacheKey);
    }

    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse == null) {
      logCacheMiss(request, cacheKey);
      return networkResponse;
    } else if (!cache.isStale(cacheResponse) || shouldReturnStaleCache(request)) {
      logCacheHit(request, cacheKey);
      return cacheResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .networkResponse(strip(networkResponse))
          .request(request)
          .build();
    }

    return networkResponse.newBuilder()
        .cacheResponse(Utils.strip(cacheResponse))
        .build();
  }

  private Response cacheFirst(Request request, Chain chain, String cacheKey) throws IOException {
    Response cacheResponse = cache.read(cacheKey, shouldExpireAfterRead(request));
    if (cacheResponse == null || cache.isStale(cacheResponse)) {
      logCacheMiss(request, cacheKey);
      if (cacheResponse != null) {
        cacheResponse.close();
      }

      Response networkResponse = withServedDateHeader(chain.proceed(request));
      if (isPrefetchResponse(request)) {
        return prefetch(networkResponse, cacheKey);
      } else if (networkResponse.isSuccessful()) {
        return cache.cacheProxy(networkResponse, cacheKey);
      }
      return networkResponse.newBuilder()
          .cacheResponse(strip(cacheResponse))
          .request(request)
          .build();
    }

    logCacheHit(request, cacheKey);
    return cacheResponse.newBuilder()
        .cacheResponse(strip(cacheResponse))
        .request(request)
        .build();
  }

  private Response prefetch(Response networkResponse, String cacheKey) throws IOException {
    cache.write(networkResponse, cacheKey);
    Response cacheResponse = cache.read(cacheKey);
    if (cacheResponse == null) {
      throw new IOException("failed to read prefetch cache response");
    }
    return cacheResponse
        .newBuilder()
        .networkResponse(Utils.strip(networkResponse))
        .build();
  }

  private void logCacheHit(Request request, String cacheKey) {
    logger.d("Cache HIT for request: %s, with cache key: %s", request, cacheKey);
  }

  private void logCacheMiss(Request request, String cacheKey) {
    logger.d("Cache MISS for request: %s, with cache key: %s", request, cacheKey);
  }
}
