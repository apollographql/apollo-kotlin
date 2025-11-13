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
import kotlin.jvm.JvmOverloads
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

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
 * Because it is hard to determine whether an exception is recoverable and because different apps may have different timeout/retry
 * requirements, customizing this interceptor using a [RetryStrategy] is encouraged.
 *
 * @see [com.apollographql.apollo.ApolloClient.Builder.retryOnErrorInterceptor]
 * @see [ApolloRequest.retryOnError]
 */
@JvmOverloads
fun RetryOnErrorInterceptor(
    networkMonitor: NetworkMonitor? = null,
    retryStrategy: RetryStrategy = defaultRetryStrategy,
): ApolloInterceptor =
  DefaultRetryOnErrorInterceptorImpl(networkMonitor, retryStrategy)

fun interface RetryStrategy {
  /**
   * Determines whether the request should be retried.
   * This function may suspend.
   *
   * @param context the retry state for a given request.
   * @return true if this request should be retried.
   */
  suspend fun shouldRetry(context: RetryContext): Boolean
}

/**
 * The retry context of this request.
 */
class RetryContext(
    val networkMonitor: NetworkMonitor?,
    /**
     * The request that needs to be retried
     */
    val request: ApolloRequest<*>,
) {
  internal var _attempt: Int = 0

  internal lateinit var _response: ApolloResponse<*>

  /**
   * The last response that was seen
   */
  val response: ApolloResponse<*>
    get() = _response

  /**
   * Reset the value of [attempt]
   */
  fun resetAttempt() {
    _attempt = 0
  }

  /**
   * The attempt number, starting from 0
   */
  val attempt: Int
    get() = _attempt
}

val defaultRetryStrategy = RetryStrategy { state: RetryContext ->
  val request = state.request
  val exception = state.response.exception
  if (request.retryOnError != true || exception == null) {
    return@RetryStrategy false
  }
  if (exception is ApolloOfflineException) {
    // We are offline
    if (request.operation is Subscription<*>) {
      state.networkMonitor!!.waitForNetwork()
      state.resetAttempt()
      return@RetryStrategy true
    } else {
      return@RetryStrategy false
    }
  }

  if (exception.isRecoverable()) {
    if (request.operation !is Subscription && state.attempt >= 3) {
      // We have waited 1 + 2 + 4 = 7 seconds.
      // Give up and return the error.
      return@RetryStrategy false
    }

    // Cap the delay at 60 seconds
    delay(2.0.pow(state.attempt).coerceAtMost(60.0).seconds)
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

    val state = RetryContext(networkMonitor, request)

    // Do not move this down into flow{} because WebSocketNetworkTransport saves some state in there
    val downstream = chain.proceed(request)

    return flow {
      if (failFastIfOffline && networkMonitor?.isOnline?.value == false) {
        emit((ApolloResponse.Builder(request.operation, request.requestUuid).exception(ApolloOfflineException()).build()))
      } else {
        emitAll(downstream)
      }
    }.onEach {
      state._response = it
      if (retryStrategy.shouldRetry(state)) {
        state._attempt++
        throw RetryException()
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
