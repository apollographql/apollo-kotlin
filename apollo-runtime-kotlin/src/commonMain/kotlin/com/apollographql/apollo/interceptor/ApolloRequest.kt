package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.ScalarTypeAdapters

@ApolloExperimental
class ApolloRequest<T> constructor(
    val operation: Operation<*, T, *>,
    val scalarTypeAdapters: ScalarTypeAdapters,
    val executionContext: ExecutionContext = ExecutionContext.Empty
)
