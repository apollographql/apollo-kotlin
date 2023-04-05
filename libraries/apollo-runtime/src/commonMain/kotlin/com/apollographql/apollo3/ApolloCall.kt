package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.DefaultApolloException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList

/**
 * An [ApolloCall] is a thin class that builds a [ApolloRequest] and calls [ApolloClient].execute() with it.
 * [ApolloCall] is mutable and designed to allow chaining calls.
 */
class ApolloCall<D : Operation.Data> internal constructor(
    internal val apolloClient: ApolloClient,
    val operation: Operation<D>,
) : MutableExecutionOptions<ApolloCall<D>> {
  override var executionContext: ExecutionContext = ExecutionContext.Empty
    private set
  override var httpMethod: HttpMethod? = null
    private set
  override var sendApqExtensions: Boolean? = null
    private set
  override var sendDocument: Boolean? = null
    private set
  override var enableAutoPersistedQueries: Boolean? = null
    private set
  override var canBeBatched: Boolean? = null
    private set
  
  /**
   * The HTTP headers to be sent with the request.
   * By default, these are *added* on top of any HTTP header previously set on [ApolloClient]. Call [ignoreApolloClientHttpHeaders]`(true)`
   * to instead *ignore* the ones set on [ApolloClient].
   */
  override var httpHeaders: List<HttpHeader>? = null
    private set

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
   * Returns a cold Flow that produces [ApolloResponse]s from this [ApolloCall].
   * Note that the execution happens when collecting the Flow.
   * This method can be called several times to execute a call again.
   *
   * The returned [Flow] does not throw unless [ApolloClient.useV3ExceptionHandling] is set to true.
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
   * Retrieve a single [ApolloResponse] from this [ApolloCall], ignoring any cache misses or network errors.
   *
   * Use this for queries and mutations to get a single value from the network or the cache.
   * For subscriptions or operations using `@defer`, you usually want to use [toFlow] instead to listen to all values.
   *
   * @throws ApolloException if the call returns zero or multiple valid GraphQL responses.
   */
  suspend fun execute(): ApolloResponse<D> {
    val responses = toFlow().toList()
    val (exceptionResponses, successResponses) = responses.partition { it.exception != null }
    return when (successResponses.size) {
      0 -> {
        when (exceptionResponses.size) {
          0 -> throw DefaultApolloException("The operation did not emit any item, check your interceptor chain")
          1 -> exceptionResponses.first()
          else -> {
            val first = exceptionResponses.first()
            first.newBuilder()
                .exception(
                    exceptionResponses.drop(1).fold(first.exception!!) { acc, response ->
                      acc.also {
                        it.addSuppressed(response.exception!!)
                      }
                    }
                )
                .build()
          }
        }
      }

      1 -> successResponses.first()
      else -> throw DefaultApolloException("The operation returned multiple items, use .toFlow() instead of .execute()")
    }
  }
}
