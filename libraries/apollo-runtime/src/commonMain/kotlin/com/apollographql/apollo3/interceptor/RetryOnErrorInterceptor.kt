package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.SubscriptionOperationException
import com.apollographql.apollo3.network.AlwaysOnlineNetworkMonitor
import com.apollographql.apollo3.network.NetworkMonitor
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

internal class RetryOnErrorInterceptor(private val networkMonitor: NetworkMonitor): ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    var first = true
    var attempt = 0
    return flow {
      val actualRequest = if (first) {
        first = false
        request
      } else {
        request.newBuilder().requestUuid(uuid4()).build()
      }

      if (request.failFastIfOffline == true && !networkMonitor.isOnline()) {
        throw OfflineException
      }

      emitAll(chain.proceed(actualRequest))
    }.catch {
      if (it == OfflineException) {
        emit(ApolloResponse.Builder(request.operation, request.requestUuid).exception(OfflineException).build())
      } else {
        throw it
      }
    }.onEach {
      if (request.retryOnError == true && it.exception != null && it.exception!!.isTerminalAndRecoverable()) {
        throw RetryException
      } else {
        attempt = 0
      }
    }.retryWhen { cause, _ ->
      if (cause is RetryException) {
        attempt++
        if (networkMonitor != AlwaysOnlineNetworkMonitor) {
          networkMonitor.waitForNetwork()
        } else {
          delay(2.0.pow(attempt).seconds)
        }
        true
      } else {
        // Not a RetryException, probably a programming error, pass it through
        false
      }
    }
  }
}

private fun ApolloException.isTerminalAndRecoverable(): Boolean {
  return when (this) {
    is SubscriptionOperationException -> {
      // This means a validation error on the subscription. It is unrecoverable
      false
    }
    else -> true
  }
}

private object RetryException: Exception()
private val OfflineException = ApolloNetworkException("The device is offline")