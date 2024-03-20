package com.apollographql.apollo3

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_8_3
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single

/**
 * An [ApolloCall] is a thin class that builds a [ApolloRequest] and calls [ApolloClient].execute() with it.
 * [ApolloCall] is mutable and designed to allow chaining calls.
 */
class ApolloCall<D : Operation.Data> internal constructor(
    internal val apolloClient: ApolloClient,
    val operation: Operation<D>,
) : MutableExecutionOptions<ApolloCall<D>> {
  override var executionContext: ExecutionContext = ExecutionContext.Empty
    @Deprecated("Use addExecutionContext() instead")
    @ApolloDeprecatedSince(v3_8_3)
    set

  override var httpMethod: HttpMethod? = null
    @Deprecated("Use httpMethod() instead")
    @ApolloDeprecatedSince(v3_8_3)
    set

  override var sendApqExtensions: Boolean? = null
    @Deprecated("Use sendApqExtensions() instead")
    @ApolloDeprecatedSince(v3_8_3)
    set

  override var sendDocument: Boolean? = null
    @Deprecated("Use sendDocument() instead")
    @ApolloDeprecatedSince(v3_8_3)
    set

  override var enableAutoPersistedQueries: Boolean? = null
    @Deprecated("Use enableAutoPersistedQueries() instead")
    @ApolloDeprecatedSince(v3_8_3)
    set

  override var httpHeaders: List<HttpHeader>? = null
    @Deprecated("Use httpHeaders() instead")
    @ApolloDeprecatedSince(v3_8_3)
    set

  private var ignoreApolloClientHttpHeaders: Boolean? = null

  override fun addExecutionContext(executionContext: ExecutionContext) = apply {
    @Suppress("DEPRECATION")
    this.executionContext = this.executionContext + executionContext
  }

  override fun httpMethod(httpMethod: HttpMethod?) = apply {
    @Suppress("DEPRECATION")
    this.httpMethod = httpMethod
  }

  /**
   * Sets the HTTP headers to be sent with the request.
   * This method overrides any HTTP header previously set on [ApolloClient]
   */
  override fun httpHeaders(httpHeaders: List<HttpHeader>?) = apply {
    check(ignoreApolloClientHttpHeaders == null) {
      "Apollo: it is an error to call both .headers() and .addHeader() or .additionalHeaders() at the same time"
    }
    @Suppress("DEPRECATION")
    this.httpHeaders = httpHeaders
  }

  /**
   * Adds an HTTP header to be sent with the request.
   * This HTTP header is added on top of any existing [ApolloClient] header. If you want to replace the
   * headers, use [httpHeaders] instead.
   */
  override fun addHttpHeader(name: String, value: String) = apply {
    check(httpHeaders == null || ignoreApolloClientHttpHeaders == false) {
      "Apollo: it is an error to call both .headers() and .addHeader() or .additionalHeaders() at the same time"
    }
    ignoreApolloClientHttpHeaders = false
    @Suppress("DEPRECATION")
    this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
  }

  override fun sendApqExtensions(sendApqExtensions: Boolean?) = apply {
    @Suppress("DEPRECATION")
    this.sendApqExtensions = sendApqExtensions
  }

  override fun sendDocument(sendDocument: Boolean?) = apply {
    @Suppress("DEPRECATION")
    this.sendDocument = sendDocument
  }

  override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?) = apply {
    @Suppress("DEPRECATION")
    this.enableAutoPersistedQueries = enableAutoPersistedQueries
  }

  override var canBeBatched: Boolean? = null
    @Deprecated("Use canBeBatched() instead")
    @ApolloDeprecatedSince(v3_8_3)
    set

  override fun canBeBatched(canBeBatched: Boolean?) = apply {
    @Suppress("DEPRECATION")
    this.canBeBatched = canBeBatched
  }

  private fun ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders: Boolean?) = apply {
    this.ignoreApolloClientHttpHeaders = ignoreApolloClientHttpHeaders
  }

  fun copy(): ApolloCall<D> {
    return ApolloCall(apolloClient, operation)
        .addExecutionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
  }

  /**
   * Returns a cold Flow that produces [ApolloResponse]s for this [ApolloCall].
   * Note that the execution happens when collecting the Flow.
   * This method can be called several times to execute a call again.
   *
   * Example:
   * ```
   * apolloClient.subscription(NewOrders())
   *                  .toFlow()
   *                  .collect {
   *                    println("order received: ${it.data?.order?.id"})
   *                  }
   * ```
   */
  fun toFlow(): Flow<ApolloResponse<D>> {
    val request = ApolloRequest.Builder(operation)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .build()
    return apolloClient.executeAsFlow(request, ignoreApolloClientHttpHeaders = ignoreApolloClientHttpHeaders == null || ignoreApolloClientHttpHeaders == true)
  }

  /**
   * A shorthand for `toFlow().single()`.
   * Use this for queries and mutation to get a single [ApolloResponse] from the network or the cache.
   * For subscriptions, you usually want to use [toFlow] instead to listen to all values.
   */
  suspend fun execute(): ApolloResponse<D> {
    return toFlow().single()
  }
}
