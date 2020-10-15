package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.benasher44.uuid.uuid4

@ApolloExperimental
class ApolloRequest<D : Operation.Data>(
    val operation: Operation<D, *>,
    val scalarTypeAdapters: ScalarTypeAdapters,
    val executionContext: ExecutionContext
) {
  val requestUuid = uuid4()
}
