package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.interceptor.ApolloRequestInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

object TestLoggerExecutor : ApolloRequestInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    println("Preparing `${request.operation.name()}` GraphQL operation for execution ...")
    return chain.proceed(request)
        .onStart { println("Started `${request.operation.name()}` GraphQL operation execution ...") }
        .onEach { response -> println("Finished `${request.operation.name()}` GraphQL operation execution, response: $response") }
        .catch { e ->
          println("Failed `${request.operation.name()}` GraphQL operation execution due to error: $e")
          throw e
        }
  }
}
