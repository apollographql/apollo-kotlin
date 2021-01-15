package com.apollographql.apollo.testing

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.ApolloRequest
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo.interceptor.ApolloResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

@ApolloExperimental
@ExperimentalCoroutinesApi
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
