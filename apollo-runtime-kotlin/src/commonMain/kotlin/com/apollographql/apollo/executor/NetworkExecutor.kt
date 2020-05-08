package com.apollographql.apollo.executor

import com.apollographql.apollo.ApolloError
import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.context.DispatchersContext
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.network.NetworkRequest
import com.apollographql.apollo.network.NetworkResponse
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

@ApolloExperimental
@ExperimentalCoroutinesApi
class NetworkExecutor(
    private val networkTransport: NetworkTransport
) : RequestExecutor {

  override fun <T> execute(request: ExecutionRequest<T>, executorChain: RequestExecutorChain): Flow<Response<T>> {
    return flow { emit(request.toNetworkRequest()) }
        .flatMapLatest { networkRequest -> networkRequest.execute(request.executionContext) }
        .map { networkResponse -> networkResponse.parse(request) }
        .flowOn(request.executionContext.ioDispatcher)
  }

  private fun ExecutionRequest<*>.toNetworkRequest(): NetworkRequest {
    return try {
      NetworkRequest(
          operationName = operation.name().name(),
          document = operation.queryDocument(),
          variables = operation.variables().marshal(scalarTypeAdapters)
      )
    } catch (e: Exception) {
      throw ApolloException(
          error = ApolloError.SerializationError("Failed to compose GraphQL network request"),
          executionContext = executionContext,
          cause = e
      )
    }
  }

  private fun NetworkRequest.execute(executionContext: ExecutionContext): Flow<NetworkResponse> {
    return networkTransport
        .execute(this)
        .catch { cause ->
          if (cause !is ApolloException) {
            throw ApolloException(
                error = ApolloError.Network("Failed to perform GraphQL network request"),
                executionContext = executionContext,
                cause = cause
            )
          } else {
            throw cause
          }
        }
  }

  private fun <T> NetworkResponse.parse(request: ExecutionRequest<T>): Response<T> {
    val response = try {
      request.operation.parse(
          source = body,
          scalarTypeAdapters = request.scalarTypeAdapters
      )
    } catch (e: Exception) {
      throw ApolloException(
          error = ApolloError.ParseError("Failed to parse GraphQL network response"),
          executionContext = request.executionContext,
          cause = e
      )
    }
    return response.copy(executionContext = response.executionContext + executionContext)
  }

  private val ExecutionContext.ioDispatcher: CoroutineDispatcher
    get() {
      val dispatchersContext = this[DispatchersContext]
      checkNotNull(dispatchersContext) {
        "Missing IO dispatcher from `${DispatchersContext::class.simpleName}` to perform network operation"
      }
      return dispatchersContext.ioDispatcher
    }
}
