package com.apollographql.apollo3.network

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import kotlinx.coroutines.flow.Flow

interface NetworkTransport {

  fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>>

  fun dispose()
}
