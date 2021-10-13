package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.withHttpMethod
import com.apollographql.apollo3.api.http.withSendApqExtensions
import com.apollographql.apollo3.api.http.withSendDocument
import com.apollographql.apollo3.exception.AutoPersistedQueriesNotSupported
import com.apollographql.apollo3.withAutoPersistedQueryInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

internal class AutoPersistedQueryInterceptor(val httpMethodForDocumentQueries: HttpMethod) : ApolloInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return flow {

      var response = chain.proceed(request).single()

      when {
        isPersistedQueryNotFound(response.errors) -> {
          val retryRequest = request
              .newBuilder()
              .withHttpMethod(httpMethodForDocumentQueries)
              .withSendDocument(true)
              .withSendApqExtensions(true)
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