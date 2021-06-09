package com.apollographql.apollo3.testing

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.interceptor.GraphQLInterceptorChain
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.interceptor.GraphQLInterceptor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

object TestLoggerExecutor : GraphQLInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: GraphQLInterceptorChain): Flow<ApolloResponse<D>> {
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
