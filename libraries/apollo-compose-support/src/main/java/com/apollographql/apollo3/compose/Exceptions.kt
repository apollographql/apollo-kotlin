package com.apollographql.apollo3.compose

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.annotations.ApolloInternal
import com.apollographql.apollo3.ApolloCall
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloException
import com.benasher44.uuid.uuid4

@ApolloExperimental
val ApolloResponse<*>.exception: ApolloException?
  get() = executionContext[ExceptionElement]?.exception

private class ExceptionElement(val exception: ApolloException) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<ExceptionElement>
}

@ApolloInternal
fun <D : Operation.Data> ApolloResponse(call: ApolloCall<D>, exception: ApolloException) =
    ApolloResponse.Builder(operation = call.operation, requestUuid = uuid4(), data = null)
        .addExecutionContext(ExceptionElement(exception))
        .build()

@ApolloInternal
suspend fun <D : Operation.Data> ApolloCall<D>.tryExecute(): ApolloResponse<D> = try {
  execute()
} catch (e: ApolloException) {
  ApolloResponse(call = this, exception = e)
}
