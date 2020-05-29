package com.apollographql.apollo.network

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.benasher44.uuid.Uuid
import okio.BufferedSource

@ApolloExperimental
class GraphQLResponse(
    val requestUuid: Uuid,
    val body: BufferedSource,
    val executionContext: ExecutionContext
)
