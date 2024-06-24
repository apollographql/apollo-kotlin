package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.network.NetworkMonitor
import com.benasher44.uuid.uuid4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds


/**
 * Returns an [ApolloInterceptor] that monitors network errors and possibly retries the [Flow] when an [ApolloNetworkException] happens.
 *
 * The returned [RetryOnErrorInterceptor]:
 * - allocates a new [ApolloRequest.requestUuid] for each retry.
 * - if [ApolloRequest.retryOnError] is `true`, waits until network is available and retries the request.
 * - if [ApolloRequest.failFastIfOffline] is `true` and [NetworkMonitor.isOnline] is `false`, returns early with [ApolloNetworkException].
 *
 * Use with [com.apollographql.apollo3.ApolloClient.Builder.retryOnErrorInterceptor]:
 *
 * ```
 * apolloClient = ApolloClient.Builder()
 *                 .serverUrl("https://...")
 *                 .retryOnErrorInterceptor(RetryOnErrorInterceptor(NetworkMonitor(context)))
 *                 .build()
 * ```
 *
 * Some other types of error than [ApolloNetworkException] might be recoverable as well (rate limit, ...) but are out of scope for this interceptor.
 *
 * @see [com.apollographql.apollo3.ApolloClient.Builder.retryOnErrorInterceptor]
 * @see [ApolloRequest.retryOnError]
 * @see [ApolloRequest.failFastIfOffline]
 */
@ApolloExperimental
fun RetryOnErrorInterceptor(networkMonitor: NetworkMonitor): ApolloInterceptor = DefaultRetryOnErrorInterceptorImpl(networkMonitor)

internal fun RetryOnErrorInterceptor(): ApolloInterceptor = DefaultRetryOnErrorInterceptorImpl(null)

private class DefaultRetryOnErrorInterceptorImpl(private val networkMonitor: NetworkMonitor?) : ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    val failFastIfOffline = request.failFastIfOffline ?: false
    val retryOnError = request.retryOnError ?: false

    if (!failFastIfOffline && !retryOnError) {
      return chain.proceed(request)
    }

    var attempt = 0
    return request.ensureUniqueUuid()
        .flatMapConcatPolyfill {
          if (failFastIfOffline && networkMonitor?.isOnline() == false) {
            flowOf(ApolloResponse.Builder(request.operation, request.requestUuid).exception(OfflineException).build())
          } else {
            chain.proceed(it)
          }
        }.onEach {
          if (retryOnError && it.exception != null && it.exception!!.isRecoverable()) {
            throw RetryException
          } else {
            attempt = 0
          }
        }.retryWhen { cause, _ ->
          if (cause is RetryException) {
            attempt++
            if (networkMonitor != null) {
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

@ApolloExperimental
internal fun <D : Operation.Data> ApolloRequest<D>.ensureUniqueUuid(): Flow<ApolloRequest<D>> {
  var first = true
  return flow {
    if (first) {
      first = false
      emit(this@ensureUniqueUuid)
    } else {
      emit(newBuilder().requestUuid(uuid4()).build())
    }
  }
}

private fun ApolloException.isRecoverable(): Boolean {
  /**
   * TODO: refine this. Some networks errors are probably not recoverable (SSL errors probably, maybe others?)
   */
  return this is ApolloNetworkException
}

private object RetryException : Exception()

private val OfflineException = ApolloNetworkException("The device is offline")

/**
 * A copy/paste of the kotlinx.coroutines version until it becomes stable
 *
 * This is taken from 1.8.0
 */
internal fun <T, R> Flow<T>.flatMapConcatPolyfill(transform: suspend (value: T) -> Flow<R>): Flow<R> =
    map(transform).flattenConcatPolyfill()

internal fun <T> Flow<Flow<T>>.flattenConcatPolyfill(): Flow<T> = flow {
  collect { value -> emitAll(value) }
}

