package com.apollographql.apollo.interceptor

import com.apollographql.apollo.ApolloHttpException
import com.apollographql.apollo.ApolloBearerTokenException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.network.HttpExecutionContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ApolloExperimental
class BearerTokenInterceptor(private val tokenProvider: TokenProvider) : ApolloRequestInterceptor {
  private val mutex = Mutex()

  private fun <D : Operation.Data> ApolloRequest<D>.withHeader(name: String, value: String): ApolloRequest<D> {
    val httpRequestContext = (executionContext[HttpExecutionContext.Request]
        ?: HttpExecutionContext.Request(emptyMap()))
        .let {
          it.copy(headers = it.headers + (name to value))
        }

    return ApolloRequest(
        operation = operation,
        customScalarAdapters = customScalarAdapters,
        executionContext = executionContext + httpRequestContext
    )
  }

  private fun <D : Operation.Data> proceedWithToken(
      request: ApolloRequest<D>,
      interceptorChain: ApolloInterceptorChain,
      token: String
  ): Flow<ApolloResponse<D>> {
    val newRequest = request.withHeader("Authorization", "Bearer $token")
    return interceptorChain.proceed(newRequest)
  }

  @FlowPreview
  @ExperimentalCoroutinesApi
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val token = mutex.withLock { tokenProvider.currentToken() }
      emit(token)
    }.flatMapConcat { token ->
      proceedWithToken(request, chain, token).catch { exception ->
        if (exception is ApolloHttpException && exception.statusCode == 401) {
          throw ApolloBearerTokenException(message = "Request failed with status code `401`", cause = exception, token = token)
        } else {
          throw exception
        }
      }
    }.retry(retries = 1) { error ->
      if (error is ApolloBearerTokenException) {
        tokenProvider.refreshToken(error.token)
        true
      } else {
        false
      }
    }
  }
}
