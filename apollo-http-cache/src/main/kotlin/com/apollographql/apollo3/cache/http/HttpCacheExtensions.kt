package com.apollographql.apollo3.cache.http

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.RequestContext
import com.apollographql.apollo3.api.ResponseContext
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposerParams
import com.apollographql.apollo3.api.http.HttpRequestComposerParams
import com.apollographql.apollo3.cache.http.internal.FileSystem
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.http.HttpResponseInfo
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

fun ApolloClient.withHttpCache(
    directory: File,
    maxSize: Long,
): ApolloClient {
  val networkTransport = networkTransport
  check(networkTransport is HttpNetworkTransport) {
    "withHttpCache requires a HttpNetworkTransport"
  }
  return copy(
      networkTransport = networkTransport.swapEngine(
        newEngine = CachingHttpEngine(
            directory = directory,
            maxSize = maxSize,
            delegate = networkTransport.engine
        )
      )
  )
}

fun <D: Query.Data> ApolloRequest<D>.withHttpFetchPolicy(httpFetchPolicy: HttpFetchPolicy): ApolloRequest<D> {
  val context = executionContext[HttpRequestComposerParams] ?: DefaultHttpRequestComposerParams

  val policyStr = when(httpFetchPolicy) {
    HttpFetchPolicy.CacheFirst -> CachingHttpEngine.CACHE_FIRST
    HttpFetchPolicy.CacheOnly -> CachingHttpEngine.CACHE_ONLY
    HttpFetchPolicy.NetworkFirst -> CachingHttpEngine.NETWORK_FIRST
    HttpFetchPolicy.NetworkOnly -> CachingHttpEngine.NETWORK_ONLY
  }
  return withExecutionContext(context.copy(headers = context.headers + (CachingHttpEngine.CACHE_FETCH_POLICY_HEADER to policyStr)))
}

val <D : Operation.Data> ApolloResponse<D>.isFromHttpCache
  get() = executionContext[HttpResponseInfo]?.headers?.any {
    // This will return true whatever the value in the header. We might want to fine tune this
    it.name == CachingHttpEngine.FROM_CACHE
  } ?: false

fun <D: Query.Data> ApolloRequest<D>.withHttpExpireTimeout(millis: Long): ApolloRequest<D> {
  val context = executionContext[HttpRequestComposerParams] ?: DefaultHttpRequestComposerParams

  return withExecutionContext(context.copy(headers = context.headers + (CachingHttpEngine.CACHE_EXPIRE_TIMEOUT_HEADER to millis.toString())))
}

fun <D: Query.Data> ApolloRequest<D>.withHttpExpireAfterRead(expireAfterRead: Boolean): ApolloRequest<D> {
  val context = executionContext[HttpRequestComposerParams] ?: DefaultHttpRequestComposerParams

  return withExecutionContext(context.copy(headers = context.headers + (CachingHttpEngine.CACHE_EXPIRE_AFTER_READ_HEADER to expireAfterRead.toString())))
}

fun <D: Query.Data> ApolloRequest<D>.withHttpDoNotStore(doNotStore: Boolean): ApolloRequest<D> {
  val context = executionContext[HttpRequestComposerParams] ?: DefaultHttpRequestComposerParams

  return withExecutionContext(context.copy(headers = context.headers + (CachingHttpEngine.CACHE_DO_NOT_STORE to doNotStore.toString())))
}