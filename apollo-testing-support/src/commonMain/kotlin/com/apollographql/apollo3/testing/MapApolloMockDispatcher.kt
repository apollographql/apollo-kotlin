package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer
import com.apollographql.apollo3.mockserver.MockRecordedRequest
import com.benasher44.uuid.uuid4

class MapApolloMockDispatcher(
    override val customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
) : ApolloMockDispatcher {
  private val operationsToResponses = mutableMapOf<Operation<out Operation.Data>, ApolloResponse<out Operation.Data>>()

  fun <D : Operation.Data> register(operation: Operation<D>, response: ApolloResponse<D>) {
    operationsToResponses[operation] = response
  }

  fun <D : Operation.Data> register(operation: Operation<D>, data: D? = null, errors: List<Error>? = null) {
    operationsToResponses[operation] = ApolloResponse.Builder(
        operation = operation,
        requestUuid = uuid4(),
        data = data
    )
        .errors(errors)
        .build()
  }

  override fun dispatch(request: MockRecordedRequest): ApolloResponse<out Operation.Data> {
    val operationId = request.headers[DefaultHttpRequestComposer.HEADER_APOLLO_OPERATION_ID]
    val operation = operationsToResponses.keys.firstOrNull { it.id() == operationId }
        ?: error("No response found for operation id: $operationId")
    return operationsToResponses[operation]!!
  }
}
