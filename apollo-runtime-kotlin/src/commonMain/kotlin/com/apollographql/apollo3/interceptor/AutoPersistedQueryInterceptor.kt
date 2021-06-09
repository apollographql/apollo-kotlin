package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpRequestComposerParams
import com.apollographql.apollo3.api.exception.AutoPersistedQueriesNotSupported
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

class AutoPersistedQueryInterceptor: ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {
      val params = request.executionContext[HttpRequestComposerParams] ?: error("no DefaultHttpRequestComposerParams found")
      var response = chain.proceed(request).single()

      when {
        isPersistedQueryNotFound(response.errors) -> {
          val retryRequest = request.withExecutionContext(params.copy(
              sendDocument = true
          ))
          response = chain.proceed(retryRequest).single()
          emit(response)
        }
        isPersistedQueryNotSupported(response.errors) -> {
          throw AutoPersistedQueriesNotSupported()
        }
        else -> {
          emit(response)
        }
      }
    }
  }

  private fun isPersistedQueryNotFound(errors: List<Error>?):Boolean {
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