package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.AutoPersistedQueryInfo
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.exception.AutoPersistedQueriesNotSupported
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

internal class AutoPersistedQueryInterceptor(
    private val httpMethodForHashedQueries: HttpMethod,
    private val httpMethodForDocumentQueries: HttpMethod,
) : ApolloInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val enabled = request.enableAutoPersistedQueries ?: true

    if (!enabled) {
      return chain.proceed(request)
    }

    @Suppress("NAME_SHADOWING")
    val request = request.newBuilder()
        .httpMethod(httpMethodForHashedQueries)
        .sendDocument(false)
        .sendApqExtensions(true)
        .build()

    return flow {
      var response = chain.proceed(request).single()

      when {
        isPersistedQueryNotFound(response.errors) -> {
          val retryRequest = request
              .newBuilder()
              .httpMethod(httpMethodForDocumentQueries)
              .sendDocument(true)
              .sendApqExtensions(true)
              .build()

          response = chain.proceed(retryRequest).single()
          emit(response.withAutoPersistedQueryInfo(false))
        }
        isPersistedQueryNotSupported(response.errors) -> {
          throw AutoPersistedQueriesNotSupported()
        }
        else -> {
          emit(response.withAutoPersistedQueryInfo(true))
        }
      }
    }
  }

  private fun <D : Operation.Data> ApolloResponse<D>.withAutoPersistedQueryInfo(hit: Boolean) = newBuilder()
      .addExecutionContext(AutoPersistedQueryInfo(hit))
      .build()

  private fun isPersistedQueryNotFound(errors: List<Error>?): Boolean {
    return errors?.any { it.message.equals(PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND, ignoreCase = true) } == true
  }

  private fun isPersistedQueryNotSupported(errors: List<Error>?): Boolean {
    return errors?.any { it.message.equals(PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED, ignoreCase = true) } == true
  }

  companion object {
    private const val PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND = "PersistedQueryNotFound"
    private const val PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED = "PersistedQueryNotSupported"
  }
}