package com.apollographql.apollo3.api

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
) : HasExecutionContext {

  fun newBuilder(): Builder<D> {
    return Builder(operation).also {
      it.requestUuid(requestUuid)
      it.executionContext = executionContext
    }
  }

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
      @Suppress("DEPRECATION")
      return ApolloRequest(
          operation = operation,
          requestUuid = requestUuid,
          executionContext = executionContext,
      )
    }
  }
}
