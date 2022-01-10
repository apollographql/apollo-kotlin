package com.apollographql.apollo3.testing

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.uuid4
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@ApolloExperimental
class QueueTestNetworkTransport : NetworkTransport {
  private val lock = reentrantLock()
  private val queue = ArrayDeque<ApolloResponse<out Operation.Data>>()

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    val response = lock.withLock { queue.removeFirstOrNull() } ?: error("No more responses in queue")
    @Suppress("UNCHECKED_CAST")
    return flowOf(response as ApolloResponse<D>)
  }

  fun <D : Operation.Data> enqueue(response: ApolloResponse<D>) {
    lock.withLock {
      queue.add(response)
    }
  }

  override fun dispose() {}
}

@ApolloExperimental
class MapTestNetworkTransport : NetworkTransport {
  private val lock = reentrantLock()
  private val operationsToResponses = mutableMapOf<Operation<out Operation.Data>, ApolloResponse<out Operation.Data>>()

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    val response = lock.withLock { operationsToResponses[request.operation] }
        ?: error("No response registered for operation ${request.operation}")
    @Suppress("UNCHECKED_CAST")
    return flowOf(response as ApolloResponse<D>)
  }

  fun <D : Operation.Data> register(operation: Operation<D>, response: ApolloResponse<D>) {
    lock.withLock {
      operationsToResponses[operation] = response
    }
  }

  override fun dispose() {}
}

@ApolloExperimental
fun <D : Operation.Data> ApolloClient.enqueueTestResponse(response: ApolloResponse<D>) =
    (networkTransport as? QueueTestNetworkTransport)?.enqueue(response)
        ?: error("Apollo: ApolloClient.enqueueTestResponse() can be used only with QueueTestNetworkTransport")

@ApolloExperimental
fun <D : Operation.Data> ApolloClient.enqueueTestResponse(
    operation: Operation<D>,
    data: D? = null,
    errors: List<Error>? = null,
) = enqueueTestResponse(
    ApolloResponse.Builder(
        operation = operation,
        requestUuid = uuid4(),
        data = data
    )
        .errors(errors)
        .build()
)

@ApolloExperimental
fun <D : Operation.Data> ApolloClient.registerTestResponse(
    operation: Operation<D>,
    response: ApolloResponse<D>,
) = (networkTransport as? MapTestNetworkTransport)?.register(operation, response)
    ?: error("Apollo: ApolloClient.registerTestResponse() can be used only with MapTestNetworkTransport")

@ApolloExperimental
fun <D : Operation.Data> ApolloClient.registerTestResponse(
    operation: Operation<D>,
    data: D? = null,
    errors: List<Error>? = null,
) = registerTestResponse(
    operation,
    ApolloResponse.Builder(
        operation = operation,
        requestUuid = uuid4(),
        data = data
    )
        .errors(errors)
        .build()
)
