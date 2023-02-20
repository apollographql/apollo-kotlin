package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.exception.ApolloCompositeException
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
  override var httpMethod: HttpMethod? = null
  override var httpHeaders: List<HttpHeader>? = null
  override var sendApqExtensions: Boolean? = null
  override var sendDocument: Boolean? = null
  override var enableAutoPersistedQueries: Boolean? = null
  override fun addExecutionContext(executionContext: ExecutionContext) = apply {
    this.executionContext = this.executionContext + executionContext
  }

  override fun httpMethod(httpMethod: HttpMethod?) = apply {
    this.httpMethod = httpMethod
  }

  override fun httpHeaders(httpHeaders: List<HttpHeader>?) = apply {
    this.httpHeaders = httpHeaders
  }

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

  override var throwOnException: Boolean? = null

  @Deprecated("Provided as a convenience to migrate from 3.x, will be removed in a future version", ReplaceWith(""))
  override fun throwOnException(throwOnException: Boolean?) = apply {
    this.throwOnException = throwOnException
  }

  fun copy(): ApolloCall<D> {
    @Suppress("DEPRECATION")
    return ApolloCall(apolloClient, operation)
        .addExecutionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .throwOnException(throwOnException)
  }

  /**
   * Returns a cold Flow that produces [ApolloResponse]s from this [ApolloCall].
   * Note that the execution happens when collecting the Flow.
   * This method can be called several times to execute a call again.
   *
   * Any [ApolloException]s will be emitted in [ApolloResponse.exception] and will not be thrown, unless [throwOnException] is set to true.
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
    @Suppress("DEPRECATION")
    val request = ApolloRequest.Builder(operation)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .throwOnException(throwOnException)
        .build()
    return apolloClient.executeAsFlow(request)
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
    val (errors, successes) = responses.partition { it.exception != null }
    return when (successes.size) {
      0 -> {
        when (errors.size) {
          0 -> throw ApolloException("The operation did not emit any item, check your interceptor chain")
          1 -> errors.first()
          else -> {
            errors[1].newBuilder()
                .exception(ApolloCompositeException(errors.map { it.exception!! }))
                .build()
          }
        }
      }

      1 -> successes.first()
      else -> throw ApolloException("The operation returned multiple items, use .toFlow() instead of .execute()")
    }
  }
}
