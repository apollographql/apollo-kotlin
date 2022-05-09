@file:JvmName("HttpCache")

package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.network.http.HttpInfo
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
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
@JvmName("configureApolloClientBuilder")
fun ApolloClient.Builder.httpCache(
    directory: File,
    maxSize: Long,
): ApolloClient.Builder {
  val cachingHttpInterceptor = CachingHttpInterceptor(
      directory = directory,
      maxSize = maxSize,
  )
  var cacheKey: String? = null
  return addHttpInterceptor(object : HttpInterceptor {
    override suspend fun intercept(request: HttpRequest, chain: HttpInterceptorChain): HttpResponse {
      cacheKey = CachingHttpInterceptor.cacheKey(request)
      return chain.proceed(request.newBuilder().addHeader(CachingHttpInterceptor.CACHE_KEY_HEADER, cacheKey!!).build())
    }
  }).addHttpInterceptor(
      cachingHttpInterceptor
  ).addInterceptor(object : ApolloInterceptor {
    override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
      return chain.proceed(
          request.newBuilder()
              .addHttpHeader(
                  CachingHttpInterceptor.CACHE_OPERATION_TYPE_HEADER,
                  when (request.operation) {
                    is Query<*> -> "query"
                    is Mutation<*> -> "mutation"
                    is Subscription<*> -> "subscription"
                    else -> error("Unknown operation type")
                  }
              )
              .build()
      ).catch { throwable ->
        // Revert caching of responses with errors
        cacheKey?.let { cachingHttpInterceptor.cache.remove(it) }
        throw throwable
      }.onEach { response ->
        // Revert caching of responses with errors
        if (response.hasErrors()) {
          cacheKey?.let { cachingHttpInterceptor.cache.remove(it) }
        }
      }
    }
  })
}

val <D : Operation.Data> ApolloResponse<D>.isFromHttpCache
  get() = executionContext[HttpInfo]?.headers?.any {
    // This will return true whatever the value in the header. We might want to fine tune this
    it.name == CachingHttpInterceptor.FROM_CACHE
  } ?: false

/**
 * Configures the [HttpFetchPolicy]
 */
fun <T> MutableExecutionOptions<T>.httpFetchPolicy(httpFetchPolicy: HttpFetchPolicy): T {
  val policyStr = when (httpFetchPolicy) {
    HttpFetchPolicy.CacheFirst -> CachingHttpInterceptor.CACHE_FIRST
    HttpFetchPolicy.CacheOnly -> CachingHttpInterceptor.CACHE_ONLY
    HttpFetchPolicy.NetworkFirst -> CachingHttpInterceptor.NETWORK_FIRST
    HttpFetchPolicy.NetworkOnly -> CachingHttpInterceptor.NETWORK_ONLY
  }

  return addHttpHeader(
      CachingHttpInterceptor.CACHE_FETCH_POLICY_HEADER, policyStr
  )
}

/**
 * Configures httpExpireTimeout. Entries will be removed from the cache after this timeout.
 */
fun <T> MutableExecutionOptions<T>.httpExpireTimeout(httpExpireTimeout: Long) = addHttpHeader(
    CachingHttpInterceptor.CACHE_EXPIRE_TIMEOUT_HEADER, httpExpireTimeout.toString()
)

/**
 * Configures httpExpireAfterRead. Entries will be removed from the cache after read if set to true.
 */
fun <T> MutableExecutionOptions<T>.httpExpireAfterRead(httpExpireAfterRead: Boolean) = addHttpHeader(
    CachingHttpInterceptor.CACHE_EXPIRE_AFTER_READ_HEADER, httpExpireAfterRead.toString()
)

/**
 * Configures httpDoNotStore. Entries will never be stored if set to true.
 */
fun <T> MutableExecutionOptions<T>.httpDoNotStore(httpDoNotStore: Boolean) = addHttpHeader(
    CachingHttpInterceptor.CACHE_DO_NOT_STORE, httpDoNotStore.toString()
)

val ApolloClient.httpCache: ApolloHttpCache
  get() {
    val httpNetworkTransport = networkTransport as? HttpNetworkTransport
        ?: error("cannot get the HttpCache, networkTransport is not a HttpNetworkTransport")
    val cachingHttpInterceptor = httpNetworkTransport.interceptors.firstOrNull { it is CachingHttpInterceptor }
        ?: error("no http cache configured")

    return (cachingHttpInterceptor as CachingHttpInterceptor).cache
  }

