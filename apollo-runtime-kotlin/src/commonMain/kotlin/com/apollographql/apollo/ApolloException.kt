package com.apollographql.apollo

import com.apollographql.apollo.api.ApolloExperimental

@ApolloExperimental
class ApolloException(
    val error: ApolloError,
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
