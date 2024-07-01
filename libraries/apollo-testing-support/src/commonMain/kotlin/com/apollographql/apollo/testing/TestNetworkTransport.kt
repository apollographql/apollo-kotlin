package com.apollographql.apollo.testing

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.network.NetworkTransport
import com.benasher44.uuid.uuid4
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield

private sealed interface TestResponse {
  object NetworkError : TestResponse
  class Response(val response: ApolloResponse<out Operation.Data>) : TestResponse
}

@ApolloExperimental
class QueueTestNetworkTransport : NetworkTransport {
  private val lock = reentrantLock()
  private val queue = ArrayDeque<TestResponse>()

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return flow {
      // "Emulate" a network call
      yield()

      val response = lock.withLock { queue.removeFirstOrNull() } ?: error("No more responses in queue")

      val apolloResponse = when (response) {
        is TestResponse.NetworkError -> {
          ApolloResponse.Builder(operation = request.operation, requestUuid = request.requestUuid)
              .exception(exception = ApolloNetworkException("Network error queued in QueueTestNetworkTransport"))
              .build()
        }

        is TestResponse.Response -> {
          @Suppress("UNCHECKED_CAST")
          response.response as ApolloResponse<D>
        }
      }

      emit(apolloResponse.newBuilder().isLast(true).build())
    }
  }

  fun <D : Operation.Data> enqueue(response: ApolloResponse<D>) {
    lock.withLock {
      queue.add(TestResponse.Response(response))
    }
  }

  fun enqueueNetworkError() {
    lock.withLock {
      queue.add(TestResponse.NetworkError)
    }
  }

  override fun dispose() {}
}

@ApolloExperimental
class MapTestNetworkTransport : NetworkTransport {
  private val lock = reentrantLock()
  private val operationsToResponses = mutableMapOf<Operation<out Operation.Data>, TestResponse>()

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return flow {
      // "Emulate" a network call
      yield()

      val response = lock.withLock { operationsToResponses[request.operation] }
          ?: error("No response registered for operation ${request.operation}")

      val apolloResponse = when (response) {
        is TestResponse.NetworkError -> {
          ApolloResponse.Builder(operation = request.operation, requestUuid = request.requestUuid)
              .exception(exception = ApolloNetworkException("Network error registered in MapTestNetworkTransport"))
              .build()
        }

        is TestResponse.Response -> {
          @Suppress("UNCHECKED_CAST")
          response.response as ApolloResponse<D>
        }
      }

      emit(apolloResponse.newBuilder().isLast(true).build())
    }
  }

  fun <D : Operation.Data> register(operation: Operation<D>, response: ApolloResponse<D>) {
    lock.withLock {
      operationsToResponses[operation] = TestResponse.Response(response)
    }
  }

  fun <D : Operation.Data> registerNetworkError(operation: Operation<D>) {
    lock.withLock {
      operationsToResponses[operation] = TestResponse.NetworkError
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
    )
        .data(data)
        .errors(errors)
        .build()
)

@ApolloExperimental
fun ApolloClient.enqueueTestNetworkError() =
    (networkTransport as? QueueTestNetworkTransport)?.enqueueNetworkError()
        ?: error("Apollo: ApolloClient.enqueueTestNetworkError() can be used only with QueueTestNetworkTransport")


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
    )
        .data(data)
        .errors(errors)
        .build()
)

@ApolloExperimental
fun <D : Operation.Data> ApolloClient.registerTestNetworkError(operation: Operation<D>) =
    (networkTransport as? MapTestNetworkTransport)?.registerNetworkError(operation)
        ?: error("Apollo: ApolloClient.registerTestNetworkError() can be used only with MapTestNetworkTransport")
