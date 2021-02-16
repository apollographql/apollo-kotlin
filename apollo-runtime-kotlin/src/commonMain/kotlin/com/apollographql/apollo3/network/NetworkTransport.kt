package com.apollographql.apollo3.network

import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.interceptor.ApolloResponse
import kotlinx.coroutines.flow.Flow

@ApolloExperimental
interface NetworkTransport {

  fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
      responseAdapterCache: ResponseAdapterCache,
      executionContext: ExecutionContext
  ): Flow<ApolloResponse<D>>
}
