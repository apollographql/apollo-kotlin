package com.apollographql.apollo

import android.os.Handler
import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import android.os.Looper
import com.apollographql.apollo.ApolloCallback
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloParseException

/**
 *
 * Android wrapper for [ApolloCall.Callback] to be operated on specified [Handler]
 *
 * **NOTE:** [.onHttpError] will be called on the background thread if provided handler is
 * attached to the main looper. This behaviour is intentional as [ApolloHttpException] internally has a reference
 * to raw [okhttp3.Response] that must be closed on the background, otherwise it throws [ ] exception.
 */
class ApolloCallback<T>(callback: ApolloCall.Callback<T>, handler: Handler) : ApolloCall.Callback<T>() {
  val delegate: ApolloCall.Callback<T>
  private val handler: Handler
  override fun onResponse(response: Response<T>) {
    handler.post { delegate.onResponse(response) }
  }

  override fun onStatusEvent(event: ApolloCall.StatusEvent) {
    handler.post { delegate.onStatusEvent(event) }
  }

  override fun onFailure(e: ApolloException) {
    handler.post { delegate.onFailure(e) }
  }

  override fun onHttpError(e: ApolloHttpException) {
    if (Looper.getMainLooper() == handler.looper) {
      delegate.onHttpError(e)
    } else {
      handler.post { delegate.onHttpError(e) }
    }
  }

  override fun onNetworkError(e: ApolloNetworkException) {
    handler.post { delegate.onNetworkError(e) }
  }

  override fun onParseError(e: ApolloParseException) {
    handler.post { delegate.onParseError(e) }
  }

  companion object {
    /**
     * Wraps `callback` to be be operated on specified `handler`
     *
     * @param callback original callback to delegates calls
     * @param handler  the callback will be run on the thread to which this handler is attached
     */
    fun <T> wrap(callback: ApolloCall.Callback<T>, handler: Handler): ApolloCallback<T> {
      return ApolloCallback(callback, handler)
    }
  }

  /**
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  init {
    delegate = __checkNotNull(callback, "callback == null")
    this.handler = __checkNotNull(handler, "handler == null")
  }
}