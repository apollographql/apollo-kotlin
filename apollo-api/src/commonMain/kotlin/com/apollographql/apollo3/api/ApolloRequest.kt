package com.apollographql.apollo3.api

import com.apollographql.apollo3.api.http.DefaultHttpRequestComposerParams
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequestComposerParams
import com.apollographql.apollo3.api.http.withHttpHeader
import com.apollographql.apollo3.api.http.withHttpMethod
import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * A GraphQL request to execute. Execution can be customized with [executionContext]
 */
class ApolloRequest<D : Operation.Data>(
    val operation: Operation<D>,
    val requestUuid: Uuid = uuid4(),
    val executionContext: ExecutionContext = ExecutionContext.Empty,
) {
  fun withExecutionContext(executionContext: ExecutionContext): ApolloRequest<D> {
    return copy(executionContext = this.executionContext + executionContext)
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
}

/**
 * Adds a HTTP header to the request
 */
fun <D : Operation.Data> ApolloRequest<D>.withHttpHeader(name: String, value: String): ApolloRequest<D> {
  val params = executionContext[HttpRequestComposerParams]
  return withExecutionContext(executionContext + params.withHttpHeader(name, value))
}

/**
 * Sets the [HttpMethod] on the request
 */
fun <D : Operation.Data> ApolloRequest<D>.withHttpMethod(method: HttpMethod): ApolloRequest<D>  {
  val params = executionContext[HttpRequestComposerParams]
  return withExecutionContext(executionContext + params.withHttpMethod(method))
}