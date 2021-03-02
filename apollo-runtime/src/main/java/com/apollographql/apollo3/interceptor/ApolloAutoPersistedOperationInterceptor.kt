package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.Error
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.Function
import com.apollographql.apollo3.api.internal.Optional
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorResponse
import java.util.concurrent.Executor

class ApolloAutoPersistedOperationInterceptor(private val logger: ApolloLogger,
                                              val useHttpGetMethodForPersistedOperations: Boolean) : ApolloInterceptor {
  @Volatile
  private var disposed = false
  override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                              dispatcher: Executor, callBack: CallBack) {
    val newRequest = request.toBuilder()
        .sendQueryDocument(false)
        .autoPersistQueries(true)
        .useHttpGetMethodForQueries(request.useHttpGetMethodForQueries || useHttpGetMethodForPersistedOperations)
        .build()
    chain.proceedAsync(newRequest, dispatcher, object : CallBack {
      override fun onResponse(response: InterceptorResponse) {
        if (disposed) return
        val retryRequest = handleProtocolNegotiation(request, response)
        if (retryRequest.isPresent) {
          chain.proceedAsync(retryRequest.get(), dispatcher, callBack)
        } else {
          callBack.onResponse(response)
          callBack.onCompleted()
        }
      }

      override fun onFetch(sourceType: FetchSourceType) {
        callBack.onFetch(sourceType)
      }

      override fun onFailure(e: ApolloException) {
        callBack.onFailure(e)
      }

      override fun onCompleted() {
        // call onCompleted in onResponse
      }
    })
  }

  override fun dispose() {
    disposed = true
  }

  fun handleProtocolNegotiation(request: InterceptorRequest,
                                response: InterceptorResponse): Optional<InterceptorRequest> {
    return (response.parsedResponse as Optional<ApolloResponse<Operation.Data>>).flatMap(object: Function<ApolloResponse<Operation.Data>, Optional<InterceptorRequest>> {
      override fun apply(response: ApolloResponse<Operation.Data>): Optional<InterceptorRequest> {
        if (response.hasErrors()) {
          if (isPersistedQueryNotFound(response.errors)) {
            logger.w("GraphQL server couldn't find Automatic Persisted Query for operation name: "
                + request.operation.name() + " id: " + request.operation.operationId())
            val retryRequest = request.toBuilder()
                .autoPersistQueries(true)
                .sendQueryDocument(true)
                .build()
            return Optional.of(retryRequest)
          }
          if (isPersistedQueryNotSupported(response.errors)) {
            // TODO how to disable Automatic Persisted Queries in future and how to notify user about this
            logger.e("GraphQL server doesn't support Automatic Persisted Queries")
            return Optional.of(request)
          }
        }
        return Optional.absent()
      }
    })
  }

  fun isPersistedQueryNotFound(errors: List<Error>?): Boolean {
    for (error in errors!!) {
      if (PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND.equals(error.message, ignoreCase = true)) {
        return true
      }
    }
    return false
  }

  fun isPersistedQueryNotSupported(errors: List<Error>?): Boolean {
    for (error in errors!!) {
      if (PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED.equals(error.message, ignoreCase = true)) {
        return true
      }
    }
    return false
  }

  class Factory @JvmOverloads constructor(val useHttpGet: Boolean = false, val persistQueries: Boolean = true, val persistMutations: Boolean = true) : ApolloInterceptorFactory {
    override fun newInterceptor(logger: ApolloLogger, operation: Operation<*>): ApolloInterceptor? {
      if (operation is Query<*> && !persistQueries) {
        return null
      }
      return if (operation is Mutation<*> && !persistMutations) {
        null
      } else ApolloAutoPersistedOperationInterceptor(logger, useHttpGet)
    }
  }

  companion object {
    private const val PROTOCOL_NEGOTIATION_ERROR_QUERY_NOT_FOUND = "PersistedQueryNotFound"
    private const val PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED = "PersistedQueryNotSupported"
  }
}
