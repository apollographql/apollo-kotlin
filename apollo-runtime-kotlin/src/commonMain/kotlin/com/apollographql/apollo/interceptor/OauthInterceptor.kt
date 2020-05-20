package com.apollographql.apollo.interceptor

import com.apollographql.apollo.ApolloError
import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.network.HttpExecutionContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@ApolloExperimental
class OauthInterceptor(private val accessTokenProvider: AccessTokenProvider) : ApolloRequestInterceptor {
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

  private suspend fun <T> proceedWithToken(request: ApolloRequest<T>, interceptorChain: ApolloInterceptorChain, token: String): Response<T> {
    val newRequest = request.withHeader("Authorization", "Bearer $token")
    return try {
      interceptorChain.proceed(newRequest).single()
    } catch (e: IllegalStateException) {
      throw ApolloException(
          message = "The downstream chain returned more than one response. Put OauthInterceptor just before the NetworkInterceptor",
          error = ApolloError.Oauth
      )
    } catch (e: NoSuchElementException) {
      throw ApolloException(
          message = "The downstream chain did not return any response.",
          error = ApolloError.Oauth
      )
    }
  }

  override fun <T> intercept(request: ApolloRequest<T>, interceptorChain: ApolloInterceptorChain): Flow<Response<T>> {
    return flow {
      val token = mutex.withLock { accessTokenProvider.currentToken() }
      emit(token)
    }.map { token ->
      proceedWithToken(request, interceptorChain, token)
    }.retry { error ->
      val shouldRenewToken = error is ApolloException && error.error is ApolloError.Network && error.executionContext[HttpExecutionContext.Response]?.statusCode == 401
      shouldRenewToken.also { renewToken ->
        if (renewToken) mutex.withLock {
          accessTokenProvider.renewToken()
        }
      }
    }
  }
}