package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.mockserver.MockRecordedRequest
import com.benasher44.uuid.uuid4

class QueueApolloMockDispatcher(
    override val customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) : ApolloMockDispatcher {
  private val queue = ArrayDeque<ApolloResponse<out Operation.Data>>()

  fun <D : Operation.Data> enqueue(response: ApolloResponse<D>) {
    queue.add(response)
  }

  fun <D : Operation.Data> enqueue(operation: Operation<D>, data: D? = null, errors: List<Error>? = null) {
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

  override fun dispatch(request: MockRecordedRequest): ApolloResponse<out Operation.Data> {
    return queue.removeFirstOrNull() ?: error("No more responses in queue")
  }
}
