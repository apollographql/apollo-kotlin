package com.apollographql.apollo.network

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import okio.BufferedSource

@ApolloExperimental
class GraphQLResponse(
    val body: BufferedSource,
    val executionContext: ExecutionContext
)
