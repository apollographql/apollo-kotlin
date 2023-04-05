package com.apollographql.apollo3

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
  override var httpMethod: HttpMethod? = null
  override var sendApqExtensions: Boolean? = null
  override var sendDocument: Boolean? = null
  override var enableAutoPersistedQueries: Boolean? = null

  /**
   * The HTTP headers to be sent with the request.
   * By default, these are *added* on top of any HTTP header previously set on [ApolloClient]. Call [ignoreApolloClientHttpHeaders]`(true)`
   * to instead *ignore* the ones set on [ApolloClient].
   */
  override var httpHeaders: List<HttpHeader>? = null

  var ignoreApolloClientHttpHeaders: Boolean? = null
    private set

  override fun addExecutionContext(executionContext: ExecutionContext) = apply {
    this.executionContext = this.executionContext + executionContext
  }

  override fun httpMethod(httpMethod: HttpMethod?) = apply {
    this.httpMethod = httpMethod
  }

  /**
   * Sets the HTTP headers to be sent with the request.
   * By default, these are *added* on top of any HTTP header previously set on [ApolloClient]. Call [ignoreApolloClientHttpHeaders]`(true)`
   * to instead *ignore* the ones set on [ApolloClient].
   */
  override fun httpHeaders(httpHeaders: List<HttpHeader>?) = apply {
    this.httpHeaders = httpHeaders
  }

  /**
   * Add an HTTP header to be sent with the request.
   * By default, these are *added* on top of any HTTP header previously set on [ApolloClient]. Call [ignoreApolloClientHttpHeaders]`(true)`
   * to instead *ignore* the ones set on [ApolloClient].
   */
  override fun addHttpHeader(name: String, value: String) = apply {
    this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
  }

  override fun sendApqExtensions(sendApqExtensions: Boolean?) = apply {
    this.sendApqExtensions = sendApqExtensions
  }

  override fun sendDocument(sendDocument: Boolean?) = apply {
    this.sendDocument = sendDocument
  }

  override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?) = apply {
    this.enableAutoPersistedQueries = enableAutoPersistedQueries
  }

  override var canBeBatched: Boolean? = null

  override fun canBeBatched(canBeBatched: Boolean?) = apply {
    this.canBeBatched = canBeBatched
  }


  /**
   * If set to true, the HTTP headers set on [ApolloClient] will not be used for the call, only the ones set on this [ApolloCall] will be
   * used. If set to false, both sets of headers will be concatenated and used.
   *
   * Default: false
   */
  fun ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders: Boolean?) = apply {
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
    return apolloClient.executeAsFlow(request, ignoreApolloClientHttpHeaders = ignoreApolloClientHttpHeaders == true)
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
