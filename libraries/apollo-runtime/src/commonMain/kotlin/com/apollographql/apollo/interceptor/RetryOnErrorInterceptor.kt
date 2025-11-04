package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloOfflineException
import com.apollographql.apollo.exception.ApolloWebSocketClosedException
import com.apollographql.apollo.network.NetworkMonitor
import com.apollographql.apollo.network.waitForNetwork
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

/**
 * Returns a default [ApolloInterceptor] that monitors exceptions and possibly retries the [Flow].
 *
 * The returned [RetryOnErrorInterceptor] uses default behavior depending on whether the current request is a subscription
 * (long-lived request) or not (short-lived request):
 * - for subscriptions:
 *   - if [ApolloResponse.exception] is an instance of [ApolloOfflineException], wait for the network to become available again and retry the request.
 *   - else if [ApolloResponse.exception] is recoverable, uses exponential backoff with a max delay of 64 seconds.
 * - for queries and mutations:
 *   - if [ApolloResponse.exception] is an instance of [ApolloOfflineException], let the exception through.
 *   - else if [ApolloResponse.exception] is recoverable, retries after 1, 3 and 7 seconds and stops retrying.
 *
 * Use with [com.apollographql.apollo.ApolloClient.Builder.retryOnErrorInterceptor]:
 *
 * ```kotlin
 * apolloClient = ApolloClient.Builder()
 *                 .serverUrl("https://...")
 *                 .retryOnErrorInterceptor(RetryOnErrorInterceptor(NetworkMonitor(context)))
 *                 .build()
 * ```
 *
 * Because it is hard to determine whether an exception is recoverable and because different apps may have different timeout/retry
 * requirements, customizing this interceptor using a [RetryStrategy] is encouraged.
 *
 * @see [com.apollographql.apollo.ApolloClient.Builder.retryOnErrorInterceptor]
 * @see [ApolloRequest.retryOnError]
 */
fun RetryOnErrorInterceptor(networkMonitor: NetworkMonitor): ApolloInterceptor =
  DefaultRetryOnErrorInterceptorImpl(networkMonitor, defaultRetryStrategy)

/**
 * Returns a default [ApolloInterceptor] that monitors exceptions and possibly retries the [Flow] according to [retryStrategy].
 *
 * Use with [com.apollographql.apollo.ApolloClient.Builder.retryOnErrorInterceptor]:
 *
 * ```kotlin
 * apolloClient = ApolloClient.Builder()
 *                 .serverUrl("https://...")
 *                 .retryOnErrorInterceptor(RetryOnErrorInterceptor(NetworkMonitor(context)))
 *                 .build()
 * ```
 *
 * @see [com.apollographql.apollo.ApolloClient.Builder.retryOnErrorInterceptor]
 * @see [ApolloRequest.retryOnError]
 */
fun RetryOnErrorInterceptor(networkMonitor: NetworkMonitor, retryStrategy: RetryStrategy): ApolloInterceptor =
  DefaultRetryOnErrorInterceptorImpl(networkMonitor, retryStrategy)

fun interface RetryStrategy {
  /**
   * Determines whether [request] should be retried.
   * This function may suspend.
   *
   * @return true if this request should be retried.
   */
  suspend fun shouldRetry(state: RetryState, request: ApolloRequest<*>, response: ApolloResponse<*>): Boolean
}

/**
 * The state of this request
 */
class RetryState(
    val networkMonitor: NetworkMonitor?,
) {
  /**
   * The current attempt, starting at 0.
   * [RetryStrategy] implementations may update this value, for an example to reset exponential backoff.
   */
  var attempt = 0
}

internal fun RetryOnErrorInterceptor(): ApolloInterceptor = DefaultRetryOnErrorInterceptorImpl(null, defaultRetryStrategy)

private val defaultRetryStrategy = RetryStrategy { state: RetryState, request: ApolloRequest<*>, response: ApolloResponse<*> ->
  val exception = response.exception
  if (exception == null) {
    // success: continue
    return@RetryStrategy false
  }

  if (exception is ApolloOfflineException) {
    // We are offline
    if (request.operation is Subscription<*>) {
      state.networkMonitor!!.waitForNetwork()
      state.attempt = 0
      return@RetryStrategy true
    } else {
      return@RetryStrategy false
    }
  }

  if (exception.isRecoverable()) {
    if (request.operation !is Subscription && state.attempt >= 3) {
      // We have waited 1 + 2 + 4 = 7 seconds
      // Give up and return the error
      return@RetryStrategy false
    }

    // Cap the delay at 60s
    delay(2.0.pow(state.attempt).coerceAtMost(60.0).seconds)
    state.attempt++
    return@RetryStrategy true
  }

  return@RetryStrategy false
}

private class DefaultRetryOnErrorInterceptorImpl(
    private val networkMonitor: NetworkMonitor?,
    private val retryStrategy: RetryStrategy,
) : ApolloInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val failFastIfOffline = request.failFastIfOffline ?: false

    val state = RetryState(networkMonitor)

    // Do not move this down into flow{} because WebSocketNetworkTransport saves some state in there
    val downstream = chain.proceed(request)

    return flow {
      if (failFastIfOffline && networkMonitor?.isOnline?.value == false) {
        emit((ApolloResponse.Builder(request.operation, request.requestUuid).exception(ApolloOfflineException()).build()))
      } else {
        emitAll(downstream)
      }
    }.onEach {
      if (request.retryOnError == true && retryStrategy.shouldRetry(state, request, it)) {
        throw RetryException()
      } else {
        state.attempt = 0
      }
    }.retryWhen { cause, _ ->
      if (cause is RetryException) {
        true
      } else {
        // Not a RetryException, probably a programming error, pass it through
        false
      }
    }
  }
}

private fun ApolloException.isRecoverable(): Boolean {
  return when (this) {
    is ApolloNetworkException -> {
      /**
       * TODO: refine this. Some networks errors are probably not recoverable (SSL errors probably, maybe others?)
       */
      true
    }

    is ApolloWebSocketClosedException -> {
      true
    }

    else -> false
  }
}

/**
 * Do not try to turn this into a singleton because the coroutine stacktrace recovery
 * mechanism may duplicate it anyway.
 */
private class RetryException : Exception("Retry")
