package com.apollographql.apollo

import com.apollographql.apollo.api.ExecutionContext

class ApolloException(
    val error: ApolloError,
    val executionContext: ExecutionContext = ExecutionContext.Empty,
    cause: Throwable? = null
) : RuntimeException(error.message, cause)
