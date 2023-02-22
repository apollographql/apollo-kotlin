package com.apollographql.apollo3.network

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import kotlinx.coroutines.flow.Flow

interface NetworkTransport {

  /**
   * Execute a request.
   *
   * This function must not throw in normal circumstances. Instead, emit an [ApolloResponse] with a non null [ApolloResponse.exception].
   * It may throw if the error is the result of a programming error like wrongly configured interceptors.
   *
   * @param request the request to execute.
   * @return a flow of responses.
   */
  fun <D : Operation.Data> execute(
      request: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>>

  fun dispose()
}
