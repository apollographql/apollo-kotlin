package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.httpHeader
import com.apollographql.apollo3.api.http.httpHeaders
import com.apollographql.apollo3.api.http.httpMethod
import com.apollographql.apollo3.api.http.sendApqExtensions
import com.apollographql.apollo3.api.http.sendDocument
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * A GraphQL request to execute. Execution can be customized with [executionContext]
 */
class ApolloRequest<D : Operation.Data> @Deprecated("Please use ApolloRequest.Builder methods instead.  This will be removed in v3.0.0.") constructor(
    val operation: Operation<D>,
    val requestUuid: Uuid,
    override val executionContext: ExecutionContext,
) : HasExecutionContext {

  fun newBuilder(): Builder<D> {
    return Builder(operation).also {
      it.requestUuid(requestUuid)
      it.executionContext = executionContext
    }
  }

  fun copy(
      operation: Operation<D> = this.operation,
      requestUuid: Uuid = this.requestUuid,
      executionContext: ExecutionContext = this.executionContext,
  ) = ApolloRequest(
      operation,
      requestUuid,
      executionContext
  )

  class Builder<D : Operation.Data>(
      private var operation: Operation<D>,
  ) : HasMutableExecutionContext<Builder<D>> {
    private var requestUuid: Uuid = uuid4()
    override var executionContext: ExecutionContext = ExecutionContext.Empty

    fun requestUuid(requestUuid: Uuid) = apply {
      this.requestUuid = requestUuid
    }

    override fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun build(): ApolloRequest<D> {
      return ApolloRequest(
          operation = operation,
          requestUuid = requestUuid,
          executionContext = executionContext,
      )
    }
  }
}

@Deprecated("Please use ApolloRequest.Builder methods instead.  This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHttpMethod(httpMethod: HttpMethod) = newBuilder().httpMethod(httpMethod).build()

@Deprecated("Please use ApolloRequest.Builder methods instead.  This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHttpHeaders(httpHeaders: List<HttpHeader>) = newBuilder().httpHeaders(httpHeaders).build()

@Deprecated("Please use ApolloRequest.Builder methods instead.  This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHttpHeader(httpHeader: HttpHeader) = newBuilder().httpHeader(httpHeader).build()

@Deprecated("Please use ApolloRequest.Builder methods instead.  This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withHttpHeader(name: String, value: String) = newBuilder().httpHeader(name, value).build()

@Deprecated("Please use ApolloRequest.Builder methods instead.  This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withSendApqExtensions(sendApqExtensions: Boolean) = newBuilder().sendApqExtensions(sendApqExtensions).build()

@Deprecated("Please use ApolloRequest.Builder methods instead.  This will be removed in v3.0.0.")
fun <D : Operation.Data> ApolloRequest<D>.withSendDocument(sendDocument: Boolean) = newBuilder().sendDocument(sendDocument).build()
