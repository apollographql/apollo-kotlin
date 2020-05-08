package com.apollographql.apollo.executor

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ScalarTypeAdapters

@ApolloExperimental
class ExecutionRequest<T> constructor(
    val operation: Operation<*, T, *>,
    val scalarTypeAdapters: ScalarTypeAdapters,
    val executionContext: ExecutionContext = ExecutionContext.Empty
)
