package com.apollographql.apollo.cache.http.internal

import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.http.HttpResponse
import com.apollographql.apollo.api.http.valueOf
import com.apollographql.apollo.cache.http.CachingHttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain

internal class CacheHeadersHttpInterceptor(private val apolloRequestToCacheKey: MutableMap<String, String>) : HttpInterceptor {
  override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
    val cacheKey = CachingHttpInterceptor.cacheKey(request)
    val requestUuid = request.headers.valueOf(CachingHttpInterceptor.REQUEST_UUID_HEADER)!!
    synchronized(apolloRequestToCacheKey) {
      apolloRequestToCacheKey[requestUuid] = cacheKey
    }
    return chain.proceed(
        request.newBuilder()
            .headers(request.headers.filterNot { it.name == CachingHttpInterceptor.REQUEST_UUID_HEADER })
            .addHeader(CachingHttpInterceptor.CACHE_KEY_HEADER, cacheKey)
            .build()
    )
  }
}
