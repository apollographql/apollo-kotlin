package com.apollographql.apollo.interceptor

import com.apollographql.apollo.AutoPersistedQueryInfo
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.exception.AutoPersistedQueriesNotSupported
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class AutoPersistedQueryInterceptor(
    private val httpMethodForHashedQueries: HttpMethod,
    private val httpMethodForDocumentQueries: HttpMethod,
) : ApolloInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val enabled = request.enableAutoPersistedQueries ?: true

    if (!enabled) {
      return chain.proceed(request)
    }

    // Always use POST for mutations to avoid hitting HTTP caches
    val isMutation = request.operation is Mutation

    @Suppress("NAME_SHADOWING")
    val request = request.newBuilder()
        .httpMethod(if (isMutation) HttpMethod.Post else httpMethodForHashedQueries)
        .sendDocument(false)
        .sendApqExtensions(true)
        .build()

    return flow {
      val responses = chain.proceed(request)
      responses.collect { response ->
        when {
          isPersistedQueryNotFound(response.errors) -> {
            val retryRequest = request
                .newBuilder()
                .httpMethod(if (isMutation) HttpMethod.Post else httpMethodForDocumentQueries)
                .sendDocument(true)
                .sendApqExtensions(true)
                .build()

            emitAll(chain.proceed(retryRequest).map { it.withAutoPersistedQueryInfo(false) })
            return@collect
          }

          isPersistedQueryNotSupported(response.errors) -> {
            emit(
                ApolloResponse.Builder(request.operation, request.requestUuid)
                    .exception(AutoPersistedQueriesNotSupported())
                    .build()
            )
            return@collect
          }

          else -> {
            emit(response.withAutoPersistedQueryInfo(true))
          }
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
    const val PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND = "PersistedQueryNotFound"
    const val PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED = "PersistedQueryNotSupported"
  }
}
