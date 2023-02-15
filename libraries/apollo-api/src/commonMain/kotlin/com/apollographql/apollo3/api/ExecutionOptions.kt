package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.exception.ApolloException

interface ExecutionOptions {
  val executionContext: ExecutionContext

  /**
   *
   * The HTTP method to use for the request
   *
   * Used by [com.apollographql.apollo3.api.http.DefaultHttpRequestComposer]
   */
  val httpMethod: HttpMethod?

  /**
   *
   * HTTP headers to use for the request
   * Used by [com.apollographql.apollo3.api.http.DefaultHttpRequestComposer]
   */
  val httpHeaders: List<HttpHeader>?

  /**
   * Whether to send the Auto Persisted Queries extensions.
   * Used by [com.apollographql.apollo3.api.http.DefaultHttpRequestComposer]
   */
  val sendApqExtensions: Boolean?

  /**
   * Whether to send the document.
   * Used by [com.apollographql.apollo3.api.http.DefaultHttpRequestComposer]
   */
  val sendDocument: Boolean?

  /**
   * Whether to enable Auto Persisted Queries and try to send a hashed query first.
   * Used by [com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor]
   */
  val enableAutoPersistedQueries: Boolean?

  /**
   * Whether the request can be batched.
   * Used by [com.apollographql.apollo3.network.http.BatchingHttpInterceptor]
   */
  val canBeBatched: Boolean?

  /**
   * Whether exceptions such as cache miss or other [ApolloException] should throw, instead of being emitted in
   * [ApolloResponse.exception].
   * Used by [com.apollographql.apollo3.ApolloCall].
   */
  val throwOnException: Boolean?

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
   * Add an HTTP header to be sent with the request.
   */
  fun addHttpHeader(name: String, value: String): T

  fun sendApqExtensions(sendApqExtensions: Boolean?): T

  fun sendDocument(sendDocument: Boolean?): T

  fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): T

  fun canBeBatched(canBeBatched: Boolean?): T

  /**
   * Configures whether exceptions such as cache miss or other [ApolloException] should throw, instead of being emitted in
   * [ApolloResponse.exception].
   *
   * If true, the call site must catch [ApolloException]. This was the behavior in Apollo Kotlin 3.
   *
   * Default: false
   */
  @Deprecated("Provided as a convenience to migrate from 3.x, will be removed in a future version")
  fun throwOnException(throwOnException: Boolean?): T
}
