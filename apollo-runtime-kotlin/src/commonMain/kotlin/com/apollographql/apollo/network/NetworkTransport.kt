package com.apollographql.apollo.network

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import kotlinx.coroutines.flow.Flow

@ApolloExperimental
interface NetworkTransport {

  fun execute(request: GraphQLRequest, executionContext: ExecutionContext): Flow<GraphQLResponse>

}
