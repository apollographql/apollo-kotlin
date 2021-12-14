package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod

interface ExecutionOptions {
  val executionContext: ExecutionContext

  /**
   * Used by [com.apollographql.apollo3.api.http.DefaultHttpRequestComposer]
   */
  val httpMethod: HttpMethod?

  /**
   * Used by [com.apollographql.apollo3.api.http.DefaultHttpRequestComposer]
   */
  val httpHeaders: List<HttpHeader>?

  /**
   * Used by [com.apollographql.apollo3.api.http.DefaultHttpRequestComposer]
   */
  val sendApqExtensions: Boolean?

  /**
   * Used by [com.apollographql.apollo3.api.http.DefaultHttpRequestComposer]
   */
  val sendDocument: Boolean?

  /**
   * Used by [com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor]
   */
  val enableAutoPersistedQueries: Boolean?

  val canBeBatched: Boolean?

  companion object {
    /**
     * Used by [com.apollographql.apollo3.network.http.BatchingHttpInterceptor]
     */
    const val CAN_BE_BATCHED = "X-APOLLO-CAN-BE-BATCHED"
  }
}


interface MutableExecutionOptions<T> : ExecutionOptions {
  fun addExecutionContext(executionContext: ExecutionContext): T

  /**
   * Configures whether the request should use GET or POST
   * Usually, POST request can transfer bigger GraphQL documents but are more difficult to cache
   *
   * Default: [HttpMethod.Post]
   */
  fun httpMethod(httpMethod: HttpMethod?): T

  /**
   * Add HTTP headers to be sent with the request.
   */
  fun httpHeaders(httpHeaders: List<HttpHeader>?): T

  /**
   * Add a HTTP header to be sent with the request.
   */
  fun addHttpHeader(name: String, value: String): T

  fun sendApqExtensions(sendApqExtensions: Boolean?): T

  fun sendDocument(sendDocument: Boolean?): T

  fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): T

  fun canBeBatched(canBeBatched: Boolean?): T
}
