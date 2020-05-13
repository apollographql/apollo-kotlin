package com.apollographql.apollo.mock

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

@ApolloExperimental
@ExperimentalCoroutinesApi
object TestLoggerExecutor : ApolloRequestInterceptor {

  override fun <T> intercept(request: ApolloRequest<T>, interceptorChain: ApolloInterceptorChain): Flow<Response<T>> {
    println("Preparing `${request.operation.name().name()}` GraphQL operation for execution ...")
    return interceptorChain.proceed(request)
        .onStart { println("Started `${request.operation.name().name()}` GraphQL operation execution ...") }
        .onEach { response -> println("Finished `${request.operation.name().name()}` GraphQL operation execution, response: $response") }
        .catch { e ->
          println("Failed `${request.operation.name().name()}` GraphQL operation execution due to error: $e")
          throw e
        }
  }
}
