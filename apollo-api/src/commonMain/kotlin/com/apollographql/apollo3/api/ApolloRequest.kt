package com.apollographql.apollo3.api

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

/**
 * A GraphQL request to execute. Execution can be customized with [executionContext]
 */
class ApolloRequest<D : Operation.Data> private constructor(
    val operation: Operation<D>,
    val requestUuid: Uuid,
    override val executionContext: ExecutionContext,
) : HasExecutionContext {

  fun newBuilder(): Builder<D> {
    return Builder(operation, requestUuid).also { it.executionContext = executionContext }
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
      var operation: Operation<D>,
      var requestUuid: Uuid = uuid4(),
  ) : ExecutionParameters<Builder<D>> {
    override var executionContext: ExecutionContext = ExecutionContext.Empty

    override fun withExecutionContext(executionContext: ExecutionContext): Builder<D> {
      this.executionContext = this.executionContext + executionContext
      return this
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
