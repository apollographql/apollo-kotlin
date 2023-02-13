package com.apollographql.apollo3.testing

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CompiledField
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.json.JsonWriter
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.uuid4
import kotlinx.atomicfu.locks.reentrantLock
import kotlinx.atomicfu.locks.withLock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.yield

@ApolloExperimental
class QueueTestNetworkTransport : NetworkTransport {
  private val lock = reentrantLock()
  private val queue = ArrayDeque<ApolloResponse<out Operation.Data>>()

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return flow {
      // "Emulate" a network call
      yield()

      @Suppress("UNCHECKED_CAST")
      val apolloResponse = lock.withLock { queue.removeFirstOrNull() } as ApolloResponse<D>? ?: error("No more responses in queue")
      emit(apolloResponse.newBuilder().isLast(true).build())
    }
  }

  fun <D : Operation.Data> enqueue(response: ApolloResponse<D>) {
    lock.withLock {
      queue.add(response)
    }
  }

  fun enqueueNetworkError() {
    lock.withLock {
      queue.add(ApolloResponse.Builder(operation = object : Operation<Operation.Data> {
        override fun document(): String = throw UnsupportedOperationException()
        override fun name(): String = throw UnsupportedOperationException()
        override fun id(): String = throw UnsupportedOperationException()
        override fun adapter(): Adapter<Operation.Data> = throw UnsupportedOperationException()
        override fun serializeVariables(
            writer: JsonWriter,
            customScalarAdapters: CustomScalarAdapters,
        ) = throw UnsupportedOperationException()

        override fun rootField(): CompiledField = throw UnsupportedOperationException()
      }, requestUuid = uuid4(), data = null)
          .exception(ApolloNetworkException("Network error queued in QueueTestNetworkTransport"))
          .build()
      )
    }
  }

  override fun dispose() {}
}

@ApolloExperimental
class MapTestNetworkTransport : NetworkTransport {
  private val lock = reentrantLock()
  private val operationsToResponses = mutableMapOf<Operation<out Operation.Data>, ApolloResponse<out Operation.Data>>()

  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    @Suppress("UNCHECKED_CAST")
    val apolloResponse = lock.withLock { operationsToResponses[request.operation] } as ApolloResponse<D>?
        ?: error("No response registered for operation ${request.operation}")
    return flowOf(apolloResponse.newBuilder().isLast(true).build())
  }

  fun <D : Operation.Data> register(operation: Operation<D>, response: ApolloResponse<D>) {
    lock.withLock {
      operationsToResponses[operation] = response
    }
  }

  fun <D : Operation.Data> registerNetworkError(operation: Operation<D>) {
    lock.withLock {
      operationsToResponses[operation] = ApolloResponse.Builder(operation = operation, requestUuid = uuid4(), data = null)
          .exception(ApolloNetworkException("Network error registered in MapTestNetworkTransport"))
          .build()
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
        data = data
    )
        .errors(errors)
        .build()
)

@ApolloExperimental
fun <D : Operation.Data> ApolloClient.registerTestNetworkError(operation: Operation<D>) =
    (networkTransport as? MapTestNetworkTransport)?.registerNetworkError(operation)
        ?: error("Apollo: ApolloClient.registerTestNetworkError() can be used only with MapTestNetworkTransport")
