package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.network.AlwaysOnlineNetworkMonitor
import com.apollographql.apollo3.network.NetworkMonitor
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds


/**
 * An [ApolloInterceptor] that monitors network errors and possibly retries the [Flow] when an [ApolloNetworkException] happens.
 *
 * Some other types of error might be recoverable as well (rate limit, ...) but are out of scope for this interceptor.
 */
internal class RetryOnNetworkErrorInterceptor(private val networkMonitor: NetworkMonitor): ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val failFastIfOffline = request.failFastIfOffline ?: false
    val retryOnError = request.retryOnError ?: false

    if (!failFastIfOffline && !retryOnError) {
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

      if (failFastIfOffline && !networkMonitor.isOnline()) {
        emit(ApolloResponse.Builder(request.operation, request.requestUuid).exception(OfflineException).build())
        return@flow
      }

      emitAll(chain.proceed(actualRequest))
    }.onEach {
      if (retryOnError && it.exception != null && it.exception!!.isRecoverable()) {
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

private fun ApolloException.isRecoverable(): Boolean {
  /**
   * TODO: refine this. Some networks errors are probably not recoverable (SSL errors probably, maybe others?)
   */
  return this is ApolloNetworkException
}

private object RetryException: Exception()
private val OfflineException = ApolloNetworkException("The device is offline")