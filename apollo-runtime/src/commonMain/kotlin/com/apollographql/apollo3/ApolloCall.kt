package com.apollographql.apollo3

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.ExecutionOptions
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single

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
    if (canBeBatched != null) addHttpHeader(ExecutionOptions.CAN_BE_BATCHED, canBeBatched.toString())
  }


  fun copy(): ApolloCall<D> {
    return ApolloCall(apolloClient, operation)
        .addExecutionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
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
        .build()
    return apolloClient.executeAsFlow(request)
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
