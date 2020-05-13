package com.apollographql.apollo.network

import com.apollographql.apollo.api.ApolloExperimental
import kotlinx.coroutines.flow.Flow

@ApolloExperimental
interface NetworkTransport {
  fun execute(request: GraphQLRequest): Flow<GraphQLResponse>
}
