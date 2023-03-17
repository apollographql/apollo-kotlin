package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.exception.ApolloException
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
  override var ignorePartialData: Boolean? = null
    private set
  override var httpHeaders: List<HttpHeader>? = null
    private set

  private var additionalHttpHeaders: List<HttpHeader>? = null

  override fun addExecutionContext(executionContext: ExecutionContext) = apply {
    this.executionContext = this.executionContext + executionContext
  }

  override fun httpMethod(httpMethod: HttpMethod?) = apply {
    this.httpMethod = httpMethod
  }

  private fun additionalHttpHeaders(additionalHttpHeaders: List<HttpHeader>?) = apply {
    this.additionalHttpHeaders = additionalHttpHeaders
  }

  /**
   * Sets the HTTP headers to be sent with the request.
   * This method overrides any HTTP header previously set on [ApolloClient]
   */
  override fun httpHeaders(httpHeaders: List<HttpHeader>?) = apply {
    this.httpHeaders = httpHeaders
  }

  /**
   * Adds an HTTP header to be sent with the request.
   * This HTTP header is added on top of any existing [ApolloClient] header. If you want to replace the
   * headers, use [httpHeaders] instead.
   */
  override fun addHttpHeader(name: String, value: String) = apply {
    this.additionalHttpHeaders = (this.additionalHttpHeaders ?: emptyList()) + HttpHeader(name, value)
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

  override fun ignorePartialData(ignorePartialData: Boolean?) = apply {
    this.ignorePartialData = ignorePartialData
  }

  fun copy(): ApolloCall<D> {
    return ApolloCall(apolloClient, operation)
        .addExecutionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .additionalHttpHeaders(additionalHttpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .ignorePartialData(ignorePartialData)
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
        .ignorePartialData(ignorePartialData)
        .build()
    return apolloClient.executeAsFlow(request, additionalHttpHeaders)
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
          0 -> throw ApolloException("The operation did not emit any item, check your interceptor chain")
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
      else -> throw ApolloException("The operation returned multiple items, use .toFlow() instead of .execute()")
    }
  }
}
