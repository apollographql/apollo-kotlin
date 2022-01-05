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
class TestNetworkTransport(
    val handler: TestNetworkTransportHandler = MapTestNetworkTransportHandler(),
) : NetworkTransport {
  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    @Suppress("UNCHECKED_CAST")
    return flowOf(handler.handle(request) as ApolloResponse<D>)
  }

  fun <D : Operation.Data> register(
      operation: Operation<D>,
      response: ApolloResponse<D>,
  ) = (handler as? MapTestNetworkTransportHandler)?.register(operation, response)
      ?: error("Apollo: cannot call TestNetworkTransport.register() with a custom handler")

  fun <D : Operation.Data> register(
      operation: Operation<D>,
      data: D? = null,
      errors: List<Error>? = null,
  ) = (handler as? MapTestNetworkTransportHandler)?.register(operation, data, errors)
      ?: error("Apollo: cannot call TestNetworkTransport.register() with a custom handler")

  override fun dispose() {}
}

@ApolloExperimental
interface TestNetworkTransportHandler {
  fun handle(request: ApolloRequest<*>): ApolloResponse<*>
}

@ApolloExperimental
class QueueTestNetworkTransportHandler : TestNetworkTransportHandler {
  private val lock = reentrantLock()
  private val queue = ArrayDeque<ApolloResponse<out Operation.Data>>()

  fun <D : Operation.Data> enqueue(response: ApolloResponse<D>) {
    lock.withLock {
      queue.add(response)
    }
  }

  fun <D : Operation.Data> enqueue(operation: Operation<D>, data: D? = null, errors: List<Error>? = null) {
    lock.withLock {
      queue.add(
          ApolloResponse.Builder(
              operation = operation,
              requestUuid = uuid4(),
              data = data
          )
              .errors(errors)
              .build()
      )
    }
  }

  override fun handle(request: ApolloRequest<*>): ApolloResponse<out Operation.Data> {
    return lock.withLock { queue.removeFirstOrNull() } ?: error("No more responses in queue")
  }
}

@ApolloExperimental
class MapTestNetworkTransportHandler : TestNetworkTransportHandler {
  private val lock = reentrantLock()
  private val operationsToResponses = mutableMapOf<Operation<out Operation.Data>, ApolloResponse<out Operation.Data>>()

  fun <D : Operation.Data> register(operation: Operation<D>, response: ApolloResponse<D>) {
    lock.withLock {
      operationsToResponses[operation] = response
    }
  }

  fun <D : Operation.Data> register(operation: Operation<D>, data: D? = null, errors: List<Error>? = null) {
    lock.withLock {
      operationsToResponses[operation] = ApolloResponse.Builder(
          operation = operation,
          requestUuid = uuid4(),
          data = data
      )
          .errors(errors)
          .build()
    }
  }

  override fun handle(request: ApolloRequest<*>): ApolloResponse<*> {
    return lock.withLock { operationsToResponses[request.operation] } ?: error("No response registered for operation ${request.operation}")
  }
}

@ApolloExperimental
val ApolloClient.testNetworkTransport: TestNetworkTransport
  get() = networkTransport as TestNetworkTransport
