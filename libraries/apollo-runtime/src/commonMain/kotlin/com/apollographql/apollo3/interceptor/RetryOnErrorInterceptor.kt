package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

@ApolloExperimental
class RetryOnErrorInterceptor(private val defaultRetryOnError: ((ApolloRequest<*>) -> Boolean)): ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val retryOnError = request.retryOnError ?: defaultRetryOnError(request)
    if (!retryOnError) {
      return chain.proceed(request)
    }

    var first = true
    var attempt = 0
    return flow {
      val actualRequest = if (first) {
        first = false
        request
      } else {
        request.newBuilder().requestUuid(uuid4()).build()
      }
      emitAll(chain.proceed(actualRequest))
    }.onEach {
      if (it.exception != null && it.exception!!.isTerminalAndRecoverable()) {
        throw RetryException
      } else {
        attempt = 0
      }
    }.retryWhen { cause, _ ->
      attempt++

      delay(2.0.pow(attempt).seconds)
      cause is RetryException
    }
  }
}


private fun ApolloException.isTerminalAndRecoverable(): Boolean {
  when (this) {
    is SubscriptionOperationException -> {
      // This often means a validation error on the subscription. It is unrecoverable
      return false
    }
    else -> return true
  }
}

private object RetryException: Exception()