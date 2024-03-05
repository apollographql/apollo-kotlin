package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * A GraphQL request to execute. Execution can be customized with [executionContext]
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
    @ApolloExperimental
    override val retryNetworkErrors: Boolean?,
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
        .retryNetworkErrors(retryNetworkErrors)
  }

  class Builder<D : Operation.Data>(
      private var operation: Operation<D>,
  ) : MutableExecutionOptions<Builder<D>> {
    private var requestUuid: Uuid = uuid4()
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
    @ApolloExperimental
    override var retryNetworkErrors: Boolean? = null
      private set

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
    override fun retryNetworkErrors(retryNetworkErrors: Boolean?): Builder<D> = apply {
      this.retryNetworkErrors = retryNetworkErrors
    }

    fun requestUuid(requestUuid: Uuid) = apply {
      this.requestUuid = requestUuid
    }

    fun executionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = executionContext
    }

    override fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun build(): ApolloRequest<D> {
      return ApolloRequest(
          operation = operation,
          requestUuid = requestUuid,
          executionContext = executionContext,
          httpMethod = httpMethod,
          httpHeaders = httpHeaders,
          sendApqExtensions = sendApqExtensions,
          sendDocument = sendDocument,
          enableAutoPersistedQueries = enableAutoPersistedQueries,
          canBeBatched = canBeBatched,
          retryNetworkErrors = retryNetworkErrors
      )
    }
  }
}
