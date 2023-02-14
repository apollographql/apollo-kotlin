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
import kotlinx.coroutines.flow.onEach
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

  fun copy(): ApolloCall<D> {
    return ApolloCall(apolloClient, operation)
        .addExecutionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
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
   * @param throwApolloExceptions whether to throw [ApolloException]s (e.g. network errors, cache misses) encountered in responses,
   * rather than emitting them in [ApolloResponse.exception]. This was the behavior in 3.x and is kept here as a convenience for migration.
   */
  fun toFlow(throwApolloExceptions: Boolean = false): Flow<ApolloResponse<D>> {
    val request = ApolloRequest.Builder(operation)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .build()
    return apolloClient.executeAsFlow(request).let {
      if (throwApolloExceptions) {
        it.onEach { response ->
          if (response.exception != null) {
            throw response.exception!!
          }
        }
      } else {
        it
      }
    }
  }

  /**
   * A shorthand for `toFlow().single()`.
   * Use this for queries and mutation to get a single [ApolloResponse] from the network or the cache.
   * For subscriptions or operations using `@defer`, you usually want to use [toFlow] instead to listen to all values.
   *
   * Exceptions (e.g. network errors, cache misses) are rethrown by this method.
   */
  suspend fun execute(): ApolloResponse<D> {
    val responses = toFlow().toList()
    val (errors, successes) = responses.partition { it.exception != null }
    if (successes.size > 1) {
      throw ApolloException("The operation returned multiple items, use .toFlow() instead of .execute()")
    } else if (successes.isEmpty()) {
      if (errors.size == 1) {
        throw errors[0].exception!!
      } else if (errors.size > 1) {
        throw ApolloCompositeException(errors[0].exception, errors[1].exception)
      } else {
        throw ApolloException("The operation did not emit any item, check your interceptor chain")
      }
    } else {
      return successes.first()
    }
  }
}
