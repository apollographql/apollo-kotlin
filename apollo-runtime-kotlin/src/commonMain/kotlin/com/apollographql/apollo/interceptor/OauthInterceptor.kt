package com.apollographql.apollo.interceptor

import com.apollographql.apollo.ApolloError
import com.apollographql.apollo.ApolloException
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.network.HttpExecutionContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

@ApolloExperimental
class OauthInterceptor(private val accessTokenProvider: AccessTokenProvider) : ApolloRequestInterceptor {
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
      val token = accessTokenProvider.currentAccessToken()

      try {
        val response = proceedWithToken(request, interceptorChain, token)
        emit(response)
      } catch (e: ApolloException) {
        if (e.error == ApolloError.Network
            && e.executionContext[HttpExecutionContext.Response]?.statusCode == 401) {
          val newToken = accessTokenProvider.newAccessToken(token)
          emit(proceedWithToken(request, interceptorChain, newToken))
        } else {
          throw e
        }
      }
    }
  }
}