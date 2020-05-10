package com.apollographql.apollo

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext

@ApolloExperimental
class ApolloException(
    override val message: String,
    cause: Throwable? = null,
    val error: ApolloError,
    val executionContext: ExecutionContext = ExecutionContext.Empty

) : RuntimeException(message, cause)
