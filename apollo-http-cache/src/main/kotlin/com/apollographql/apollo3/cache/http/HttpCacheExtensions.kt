package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.httpHeader
import com.apollographql.apollo3.network.http.HttpInfo
import com.apollographql.apollo3.network.http.HttpEngine
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

/**
 * Configures a persistent LRU HTTP cache for the ApolloClient.
 *
 * @param directory: the directory where the cache will be persisted
 * @param maxSize: the maxSize in bytes that the cache acn occupy
 *
 * See also [ApolloClient.Builder.httpEngine] and [ApolloClient.Builder.networkTransport]
 */
fun ApolloClient.Builder.httpCache(
    directory: File,
    maxSize: Long,
): ApolloClient.Builder {

  return httpEngine(
      httpEngine = CachingHttpEngine(
          directory = directory,
          maxSize = maxSize,
          delegate = HttpEngine()
      )
  )
}

val <D : Operation.Data> ApolloResponse<D>.isFromHttpCache
  get() = executionContext[HttpInfo]?.headers?.any {
    // This will return true whatever the value in the header. We might want to fine tune this
    it.name == CachingHttpEngine.FROM_CACHE
  } ?: false

/**
 * Configures the [HttpFetchPolicy]
 */
fun <T> HasMutableExecutionContext<T>.httpFetchPolicy(httpFetchPolicy: HttpFetchPolicy): T where T : HasMutableExecutionContext<T> {
  val policyStr = when (httpFetchPolicy) {
    HttpFetchPolicy.CacheFirst -> CachingHttpEngine.CACHE_FIRST
    HttpFetchPolicy.CacheOnly -> CachingHttpEngine.CACHE_ONLY
    HttpFetchPolicy.NetworkFirst -> CachingHttpEngine.NETWORK_FIRST
    HttpFetchPolicy.NetworkOnly -> CachingHttpEngine.NETWORK_ONLY
  }

  return httpHeader(
      CachingHttpEngine.CACHE_FETCH_POLICY_HEADER, policyStr
  )
}

/**
 * Configures httpExpireTimeout. Entries will be removed from the cache after this timeout.
 */
fun <T> HasMutableExecutionContext<T>.httpExpireTimeout(httpExpireTimeout: Long) where T : HasMutableExecutionContext<T> = httpHeader(
    CachingHttpEngine.CACHE_EXPIRE_TIMEOUT_HEADER, httpExpireTimeout.toString()
)

/**
 * Configures httpExpireAfterRead. Entries will be removed from the cache after read if set to true.
 */
fun <T> HasMutableExecutionContext<T>.httpExpireAfterRead(httpExpireAfterRead: Boolean) where T : HasMutableExecutionContext<T> = httpHeader(
    CachingHttpEngine.CACHE_EXPIRE_AFTER_READ_HEADER, httpExpireAfterRead.toString()
)

/**
 * Configures httpDoNotStore. Entries will never be stored if set to true.
 */
fun <T> HasMutableExecutionContext<T>.httpDoNotStore(httpDoNotStore: Boolean) where T : HasMutableExecutionContext<T> = httpHeader(
    CachingHttpEngine.CACHE_DO_NOT_STORE, httpDoNotStore.toString()
)

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpCache(
    directory: File,
    maxSize: Long,
): ApolloClient = newBuilder().httpCache(directory, maxSize).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpFetchPolicy(httpFetchPolicy: HttpFetchPolicy) = newBuilder().httpFetchPolicy(httpFetchPolicy).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHttpFetchPolicy(httpFetchPolicy: HttpFetchPolicy) = newBuilder().httpFetchPolicy(httpFetchPolicy).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpExpireTimeout(millis: Long) = newBuilder().httpExpireTimeout(millis).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHttpExpireTimeout(millis: Long) = newBuilder().httpExpireTimeout(millis).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpExpireAfterRead(expireAfterRead: Boolean) = newBuilder().httpExpireAfterRead(expireAfterRead).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHttpExpireAfterRead(expireAfterRead: Boolean) = newBuilder().httpExpireAfterRead(expireAfterRead).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpDoNotStore(doNotStore: Boolean) = newBuilder().httpDoNotStore(doNotStore).build()

@Deprecated("Please use ApolloRequest.Builder methods instead. This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHttpDoNotStore(doNotStore: Boolean) = newBuilder().httpDoNotStore(doNotStore).build()
