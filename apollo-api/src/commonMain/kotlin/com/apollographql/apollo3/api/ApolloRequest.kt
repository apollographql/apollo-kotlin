package com.apollographql.apollo3.api

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * A GraphQL request to execute. Execution can be customized with [executionContext]
 */
class ApolloRequest<D : Operation.Data>(
    val operation: Operation<D>,
    val requestUuid: Uuid = uuid4(),
    override val executionContext: ExecutionContext = ExecutionContext.Empty,
): ExecutionParameters<ApolloRequest<D>> {
  override fun withExecutionContext(executionContext: ExecutionContext): ApolloRequest<D> {
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
