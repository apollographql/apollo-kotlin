package com.apollographql.apollo3.api

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4

data class ApolloRequest<D : Operation.Data>(
    val operation: Operation<D>,
    val requestUuid: Uuid = uuid4(),
    val executionContext: ExecutionContext = ExecutionContext.Empty,
) {
  fun withExecutionContext(executionContext: ExecutionContext): ApolloRequest<D> {
    return copy(executionContext = this.executionContext + executionContext)
  }
}
