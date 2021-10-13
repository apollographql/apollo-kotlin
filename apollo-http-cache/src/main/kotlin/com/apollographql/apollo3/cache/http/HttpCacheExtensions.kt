package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.withHttpHeader
import com.apollographql.apollo3.network.http.HttpInfo
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import java.io.File


enum class HttpFetchPolicy {
  /**
   * Try cache first, then network
   *
   * This is the default behaviour
   */
  CacheFirst,

  /**
   * Only try cache
   */
  CacheOnly,

  /**
   * Try network first, then cache
   */
  NetworkFirst,

  /**
   * Only try network
   */
  NetworkOnly,
}

fun ApolloClient.Builder.httpCache(
    directory: File,
    maxSize: Long,
): ApolloClient.Builder {
  val networkTransport = networkTransport
  check(networkTransport is HttpNetworkTransport) {
    "withHttpCache requires a HttpNetworkTransport"
  }
  return networkTransport(networkTransport.copy(
          engine = CachingHttpEngine(
              directory = directory,
              maxSize = maxSize,
              delegate = networkTransport.engine
          )
      )
  )
}

val <D : Operation.Data> ApolloResponse<D>.isFromHttpCache
  get() = executionContext[HttpInfo]?.headers?.any {
    // This will return true whatever the value in the header. We might want to fine tune this
    it.name == CachingHttpEngine.FROM_CACHE
  } ?: false


fun <T> HasMutableExecutionContext<T>.withHttpFetchPolicy(httpFetchPolicy: HttpFetchPolicy): T where T : HasMutableExecutionContext<T> {
  val policyStr = when (httpFetchPolicy) {
    HttpFetchPolicy.CacheFirst -> CachingHttpEngine.CACHE_FIRST
    HttpFetchPolicy.CacheOnly -> CachingHttpEngine.CACHE_ONLY
    HttpFetchPolicy.NetworkFirst -> CachingHttpEngine.NETWORK_FIRST
    HttpFetchPolicy.NetworkOnly -> CachingHttpEngine.NETWORK_ONLY
  }

  return withHttpHeader(
      CachingHttpEngine.CACHE_FETCH_POLICY_HEADER, policyStr
  )
}

fun <T> HasMutableExecutionContext<T>.withHttpExpireTimeout(millis: Long) where T : HasMutableExecutionContext<T> = withHttpHeader(
    CachingHttpEngine.CACHE_EXPIRE_TIMEOUT_HEADER, millis.toString()
)

fun <T> HasMutableExecutionContext<T>.withHttpExpireAfterRead(expireAfterRead: Boolean) where T : HasMutableExecutionContext<T> = withHttpHeader(
    CachingHttpEngine.CACHE_EXPIRE_AFTER_READ_HEADER, expireAfterRead.toString()
)

fun <T> HasMutableExecutionContext<T>.withHttpDoNotStore(doNotStore: Boolean) where T : HasMutableExecutionContext<T> = withHttpHeader(
    CachingHttpEngine.CACHE_DO_NOT_STORE, doNotStore.toString()
)