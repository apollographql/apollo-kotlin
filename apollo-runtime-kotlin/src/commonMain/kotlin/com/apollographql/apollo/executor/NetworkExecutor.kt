package com.apollographql.apollo.executor

import com.apollographql.apollo.ApolloError
import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.network.GraphQLRequest
import com.apollographql.apollo.network.GraphQLResponse
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

@ApolloExperimental
@ExperimentalCoroutinesApi
class NetworkExecutor(
    private val networkTransport: NetworkTransport
) : RequestExecutor {

  override fun <T> execute(request: ExecutionRequest<T>, executorChain: RequestExecutorChain): Flow<Response<T>> {
    return flow { emit(request.toNetworkRequest()) }
        .flatMapLatest { networkRequest -> networkTransport.execute(networkRequest) }
        .map { networkResponse -> networkResponse.parse(request) }
  }

  private fun <T> GraphQLResponse.parse(request: ExecutionRequest<T>): Response<T> {
    val response = try {
      request.operation.parse(
          source = body,
          scalarTypeAdapters = request.scalarTypeAdapters
      )
    } catch (e: Exception) {
      throw ApolloException(
          message = "Failed to parse GraphQL network response",
          error = ApolloError.ParseError,
          executionContext = request.executionContext,
          cause = e
      )
    } finally {
      body.close()
    }
    return response.copy(executionContext = request.executionContext + response.executionContext)
  }

  private fun ExecutionRequest<*>.toNetworkRequest(): GraphQLRequest {
    return try {
      GraphQLRequest(
          operationName = operation.name().name(),
          operationId = operation.operationId(),
          document = operation.queryDocument(),
          variables = operation.variables().marshal(scalarTypeAdapters)
      )
    } catch (e: Exception) {
      throw ApolloException(
          message = "Failed to compose GraphQL network request",
          error = ApolloError.SerializationError,
          executionContext = executionContext,
          cause = e
      )
    }
  }
}
