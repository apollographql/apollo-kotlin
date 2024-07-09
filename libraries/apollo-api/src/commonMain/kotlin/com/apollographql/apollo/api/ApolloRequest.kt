package com.apollographql.apollo.api

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * An [ApolloRequest] represents a GraphQL request to execute.
 *
 * [ApolloRequest] is immutable and is usually constructed from [com.apollographql.apollo.ApolloCall].
 *
 * You can mutate an [ApolloRequest] by calling [newBuilder]:
 *
 * ```
 * val newRequest = apolloRequest.newBuilder().addHttpHeader("Authorization", "Bearer $token").build()
 * ```
 *
 * @property operation the GraphQL operation for this request
 * @property requestUuid a unique id for this request. For queries and mutations, this is only used for debug.
 * For subscriptions, it is used as subscription id when multiplexing several subscription over a WebSocket.
 *
 * @see [com.apollographql.apollo.ApolloCall]
 */
class ApolloRequest<D : Operation.Data>
private constructor(
    val operation: Operation<D>,
    val requestUuid: Uuid,
    override val executionContext: ExecutionContext,
    override val httpMethod: HttpMethod?,
    override val httpHeaders: List<HttpHeader>?,
    override val sendApqExtensions: Boolean?,
    override val sendDocument: Boolean?,
    override val enableAutoPersistedQueries: Boolean?,
    override val canBeBatched: Boolean?,
    val ignoreApolloClientHttpHeaders: Boolean?,
    @ApolloExperimental
    val retryOnError: Boolean?,
    @ApolloExperimental
    val failFastIfOffline: Boolean?,
) : ExecutionOptions {

  fun newBuilder(): Builder<D> = newBuilder(operation)

  @ApolloExperimental
  fun <E : Operation.Data> newBuilder(operation: Operation<E>): Builder<E> {
    return Builder(operation)
        .requestUuid(requestUuid)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
        .retryOnError(retryOnError)
        .failFastIfOffline(failFastIfOffline)
        .ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders)
  }

  class Builder<D : Operation.Data>(
      val operation: Operation<D>,
  ) : MutableExecutionOptions<Builder<D>> {
    var requestUuid: Uuid? = null
      private set
    override var executionContext: ExecutionContext = ExecutionContext.Empty
      private set
    override var httpMethod: HttpMethod? = null
      private set
    override var httpHeaders: List<HttpHeader>? = null
      private set
    override var enableAutoPersistedQueries: Boolean? = null
      private set
    override var sendApqExtensions: Boolean? = null
      private set
    override var sendDocument: Boolean? = null
      private set
    override var canBeBatched: Boolean? = null
      private set
    var ignoreApolloClientHttpHeaders: Boolean? = null
      private set
    @ApolloExperimental
    var retryOnError: Boolean? = null
      private set
    @ApolloExperimental
    var failFastIfOffline: Boolean? = null
      private set


    fun requestUuid(requestUuid: Uuid) = apply {
      this.requestUuid = requestUuid
    }

    fun executionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = executionContext
    }

    override fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun ignoreApolloClientHttpHeaders(ignoreApolloClientHttpHeaders: Boolean?) = apply {
      this.ignoreApolloClientHttpHeaders = ignoreApolloClientHttpHeaders
    }

    override fun httpMethod(httpMethod: HttpMethod?): Builder<D> = apply {
      this.httpMethod = httpMethod
    }

    override fun httpHeaders(httpHeaders: List<HttpHeader>?): Builder<D> = apply {
      this.httpHeaders = httpHeaders
    }

    override fun addHttpHeader(name: String, value: String): Builder<D> = apply {
      this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
    }

    override fun sendApqExtensions(sendApqExtensions: Boolean?): Builder<D> = apply {
      this.sendApqExtensions = sendApqExtensions
    }

    override fun sendDocument(sendDocument: Boolean?): Builder<D> = apply {
      this.sendDocument = sendDocument
    }

    override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): Builder<D> = apply {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
    }

    override fun canBeBatched(canBeBatched: Boolean?): Builder<D> = apply {
      this.canBeBatched = canBeBatched
    }

    @ApolloExperimental
    fun retryOnError(retryOnError: Boolean?): Builder<D> = apply {
      this.retryOnError = retryOnError
    }

    @ApolloExperimental
    fun failFastIfOffline(failFastIfOffline: Boolean?): Builder<D> = apply {
      this.failFastIfOffline = failFastIfOffline
    }

    fun build(): ApolloRequest<D> {
      return ApolloRequest(
          operation = operation,
          requestUuid = requestUuid ?: uuid4(),
          executionContext = executionContext,
          httpMethod = httpMethod,
          httpHeaders = httpHeaders,
          sendApqExtensions = sendApqExtensions,
          sendDocument = sendDocument,
          enableAutoPersistedQueries = enableAutoPersistedQueries,
          canBeBatched = canBeBatched,
          ignoreApolloClientHttpHeaders = ignoreApolloClientHttpHeaders,
          retryOnError = retryOnError,
          failFastIfOffline = failFastIfOffline,
      )
    }
  }
}
