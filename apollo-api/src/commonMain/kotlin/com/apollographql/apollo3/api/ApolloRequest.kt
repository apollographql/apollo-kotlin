package com.apollographql.apollo3.api

import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * A GraphQL request to execute. Execution can be customized with [executionContext]
 */
@OptIn(ApolloInternal::class)
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
) : ExecutionOptions {

  fun newBuilder(): Builder<D> {
    return Builder(operation)
        .requestUuid(requestUuid)
        .executionContext(executionContext)
        .httpMethod(httpMethod)
        .httpHeaders(httpHeaders)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .canBeBatched(canBeBatched)
  }

  class Builder<D : Operation.Data>(
      private var operation: Operation<D>,
  ) : MutableExecutionOptions<Builder<D>> {
    private var requestUuid: Uuid = uuid4()
    override var executionContext: ExecutionContext = ExecutionContext.Empty

    override var httpMethod: HttpMethod? = null

    override fun httpMethod(httpMethod: HttpMethod?): Builder<D> = apply {
      this.httpMethod = httpMethod
    }

    override var httpHeaders: List<HttpHeader>? = null

    override fun httpHeaders(httpHeaders: List<HttpHeader>?): Builder<D> = apply {
      this.httpHeaders = httpHeaders
    }

    override fun addHttpHeader(name: String, value: String): Builder<D> = apply {
      this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
    }

    override var sendApqExtensions: Boolean? = null

    override fun sendApqExtensions(sendApqExtensions: Boolean?): Builder<D> = apply {
      this.sendApqExtensions = sendApqExtensions
    }

    override var sendDocument: Boolean? = null

    override fun sendDocument(sendDocument: Boolean?): Builder<D> = apply {
      this.sendDocument = sendDocument
    }

    override var enableAutoPersistedQueries: Boolean? = null

    override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): Builder<D> = apply {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
    }

    override var canBeBatched: Boolean? = null

    override fun canBeBatched(canBeBatched: Boolean?): Builder<D> = apply {
      this.canBeBatched = canBeBatched
      if (canBeBatched != null) addHttpHeader(ExecutionOptions.CAN_BE_BATCHED, canBeBatched.toString())
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
      @Suppress("DEPRECATION")
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
      )
    }
  }
}
