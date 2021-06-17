package com.apollographql.apollo3.internal

import com.apollographql.apollo3.ApolloPrefetch
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.Optional
import com.apollographql.apollo3.api.exception.ApolloCanceledException
import com.apollographql.apollo3.api.exception.ApolloException
import com.apollographql.apollo3.api.exception.ApolloHttpException
import com.apollographql.apollo3.api.exception.ApolloNetworkException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.internal.CallState.IllegalStateMessage.Companion.forCurrentState
import com.apollographql.apollo3.internal.interceptor.ApolloServerInterceptor
import com.apollographql.apollo3.internal.interceptor.RealApolloInterceptorChain
import okhttp3.Call
import okhttp3.Headers
import okhttp3.HttpUrl
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

class RealApolloPrefetch(
    val operation: Operation<*>,
    val serverUrl: HttpUrl,
    val httpCallFactory: Call.Factory,
    val customScalarAdapters: CustomScalarAdapters,
    val dispatcher: Executor,
    val logger: ApolloLogger,
    val tracker: ApolloCallTracker
) : ApolloPrefetch {
  val interceptorChain: ApolloInterceptorChain
  val state = AtomicReference(CallState.IDLE)
  val originalCallback = AtomicReference<ApolloPrefetch.Callback?>()
  override fun enqueue(callback: ApolloPrefetch.Callback?) {
    try {
      activate(Optional.fromNullable(callback))
    } catch (e: ApolloCanceledException) {
      if (callback != null) {
        callback.onFailure(e)
      } else {
        logger.e(e, "Operation: %s was canceled", operation().name())
      }
      return
    }
    val request = InterceptorRequest.builder(operation).build()
    interceptorChain.proceedAsync(request, dispatcher, interceptorCallbackProxy())
  }

  override fun operation(): Operation<*> {
    return operation
  }

  private fun interceptorCallbackProxy(): CallBack {
    return object : CallBack {
      override fun onResponse(response: InterceptorResponse) {
        val httpResponse = response.httpResponse.get()
        try {
          val callback = terminate()
          if (!callback.isPresent) {
            logger.d("onResponse for prefetch operation: %s. No callback present.", operation().name())
            return
          }
          if (httpResponse.isSuccessful) {
            callback.get().onSuccess()
          } else {
            callback.get().onHttpError(ApolloHttpException(
                statusCode = httpResponse.code(),
                headers = httpResponse.headers().toMap(),
                message = httpResponse.body()?.string() ?: "",
                cause = null
            ))
          }
        } finally {
          httpResponse.close()
        }
      }

      private fun Headers.toMap(): Map<String, String> = this.names().map {
        it to get(it)!!
      }.toMap()

      override fun onFailure(e: ApolloException) {
        val callback = terminate()
        if (!callback.isPresent) {
          logger.e(e, "onFailure for prefetch operation: %s. No callback present.", operation().name())
          return
        }
        if (e is ApolloHttpException) {
          callback.get().onHttpError(e)
        } else if (e is ApolloNetworkException) {
          callback.get().onNetworkError(e)
        } else {
          callback.get().onFailure(e)
        }
      }

      override fun onCompleted() {
        // Prefetch is only called with NETWORK_ONLY, so callback api does not need onComplete as it is the same as
        // onResponse.
      }

      override fun onFetch(sourceType: FetchSourceType) {}
    }
  }

  override fun clone(): ApolloPrefetch {
    return RealApolloPrefetch(operation, serverUrl, httpCallFactory, customScalarAdapters, dispatcher, logger,
        tracker)
  }

  @Synchronized
  override fun cancel() {
    when (state.get()) {
      CallState.ACTIVE -> try {
        interceptorChain.dispose()
      } finally {
        tracker.unregisterPrefetchCall(this)
        originalCallback.set(null)
        state.set(CallState.CANCELED)
      }
      CallState.IDLE -> state.set(CallState.CANCELED)
      CallState.CANCELED, CallState.TERMINATED -> {
      }
      else -> throw IllegalStateException("Unknown state")
    }
  }

  override val isCanceled: Boolean
    get() = state.get() === CallState.CANCELED

  @Synchronized
  @Throws(ApolloCanceledException::class)
  private fun activate(callback: Optional<ApolloPrefetch.Callback>) {
    when (state.get()) {
      CallState.IDLE -> {
        originalCallback.set(callback.orNull())
        tracker.registerPrefetchCall(this)
      }
      CallState.CANCELED -> throw ApolloCanceledException()
      CallState.TERMINATED, CallState.ACTIVE -> throw IllegalStateException("Already Executed")
      else -> throw IllegalStateException("Unknown state")
    }
    state.set(CallState.ACTIVE)
  }

  @Synchronized
  fun terminate(): Optional<ApolloPrefetch.Callback> {
    return when (state.get()) {
      CallState.ACTIVE -> {
        tracker.unregisterPrefetchCall(this)
        state.set(CallState.TERMINATED)
        Optional.fromNullable(originalCallback.getAndSet(null))
      }
      CallState.CANCELED -> Optional.fromNullable(originalCallback.getAndSet(null))
      CallState.IDLE, CallState.TERMINATED -> throw IllegalStateException(
          forCurrentState(state.get()).expected(CallState.ACTIVE, CallState.CANCELED))
      else -> throw IllegalStateException("Unknown state")
    }
  }

  init {
    interceptorChain = RealApolloInterceptorChain(listOf<ApolloInterceptor>(
        ApolloServerInterceptor(serverUrl, httpCallFactory, HttpCachePolicy.NETWORK_ONLY, true, customScalarAdapters,
            logger)
    ))
  }
}
