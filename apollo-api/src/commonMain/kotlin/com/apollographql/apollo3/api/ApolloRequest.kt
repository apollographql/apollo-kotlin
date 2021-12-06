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
    override val httpMethod: HttpMethod,
    override val httpHeaders: List<HttpHeader>,
    override val sendApqExtensions: Boolean,
    override val sendDocument: Boolean,
) : ExecutionOptions {

  fun newBuilder(): Builder<D> {
    return Builder(operation).also {
      it.requestUuid(requestUuid)
      it.executionContext = executionContext
    }
  }

  class Builder<D : Operation.Data>(
      private var operation: Operation<D>,
  ) : MutableExecutionOptions<Builder<D>> {
    private var requestUuid: Uuid = uuid4()
    override var executionContext: ExecutionContext = ExecutionContext.Empty

    override var httpMethod: HttpMethod = ExecutionOptions.defaultHttpMethod

    override fun httpMethod(httpMethod: HttpMethod): Builder<D> = apply {
      this.httpMethod = httpMethod
    }

    override var httpHeaders: List<HttpHeader> = emptyList()

    override fun httpHeaders(httpHeaders: List<HttpHeader>): Builder<D> = apply {
      this.httpHeaders = httpHeaders
    }

    override var sendApqExtensions: Boolean = ExecutionOptions.defaultSendApqExtensions

    override fun sendApqExtensions(sendApqExtensions: Boolean): Builder<D> = apply {
      this.sendApqExtensions = sendApqExtensions
    }

    override var sendDocument: Boolean = ExecutionOptions.defaultSendDocument

    override fun sendDocument(sendDocument: Boolean): Builder<D> = apply {
      this.sendDocument = sendDocument
    }

    fun requestUuid(requestUuid: Uuid) = apply {
      this.requestUuid = requestUuid
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
          sendDocument = sendDocument
      )
    }
  }
}
