package com.apollographql.apollo.interceptor

import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.ApolloHttpException
import com.apollographql.apollo.BearerTokenException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.network.HttpExecutionContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ApolloExperimental
class BearerTokenInterceptor(private val tokenProvider: TokenProvider) : ApolloRequestInterceptor {
  val mutex = Mutex()

  private fun <T> ApolloRequest<T>.withHeader(name: String, value: String): ApolloRequest<T> {
    val httpRequestContext = executionContext[HttpExecutionContext.Request]
        ?: HttpExecutionContext.Request(emptyMap())
            .let {
              it.copy(headers = it.headers + (name to value))
            }

    return ApolloRequest(
        operation = operation,
        scalarTypeAdapters = scalarTypeAdapters,
        executionContext = executionContext + httpRequestContext
    )
  }

  private fun <T> proceedWithToken(request: ApolloRequest<T>, interceptorChain: ApolloInterceptorChain, token: String): Flow<Response<T>> {
    val newRequest = request.withHeader("Authorization", "Bearer $token")
    return interceptorChain.proceed(newRequest)
  }

  override fun <T> intercept(request: ApolloRequest<T>, interceptorChain: ApolloInterceptorChain): Flow<Response<T>> {
    return flow {
      val token = mutex.withLock { tokenProvider.currentToken() }
      emit(token)
    }.flatMapConcat { token ->
      proceedWithToken(request, interceptorChain, token).catch { exception->
        if (exception is ApolloHttpException && exception.statusCode == 401) {
          throw BearerTokenException(message = "Request failed with status code `401`", cause = exception, token = token)
        } else {
          throw exception
        }
      }
    }.retry(retries = 1) { error ->
      if (error is BearerTokenException) {
        tokenProvider.renewToken(error.token)
        true
      } else {
        false
      }
    }
  }
}