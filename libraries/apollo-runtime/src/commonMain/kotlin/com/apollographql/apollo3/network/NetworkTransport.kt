package com.apollographql.apollo3.network

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloException
import kotlinx.coroutines.flow.Flow

interface NetworkTransport {

  /**
   * Execute a request.
   *
   * @param request the request to execute.
   * @return a flow of responses.
   * [ApolloException]s must not be thrown from the flow. Instead, they must be emitted as [ApolloResponse.exception].
   */
  fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>>

  fun dispose()
}
