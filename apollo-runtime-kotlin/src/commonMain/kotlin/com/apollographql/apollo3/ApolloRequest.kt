package com.apollographql.apollo3

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
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
