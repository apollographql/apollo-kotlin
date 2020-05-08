package com.apollographql.apollo.mock

import com.apollographql.apollo.executor.ExecutionRequest
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.executor.RequestExecutor
import com.apollographql.apollo.executor.RequestExecutorChain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

object TestLoggerExecutor : RequestExecutor {
  override fun <T> execute(request: ExecutionRequest<T>, executorChain: RequestExecutorChain): Flow<Response<T>> {
    println("Preparing `${request.operation.name().name()}` GraphQL operation for execution ...")
    return executorChain.proceed(request)
        .onStart { println("Started `${request.operation.name().name()}` GraphQL operation execution ...") }
        .onEach { response -> println("Finished `${request.operation.name().name()}` GraphQL operation execution, response: $response") }
        .catch { e ->
          println("Failed `${request.operation.name().name()}` GraphQL operation execution due to error: $e")
          throw e
        }
  }
}
