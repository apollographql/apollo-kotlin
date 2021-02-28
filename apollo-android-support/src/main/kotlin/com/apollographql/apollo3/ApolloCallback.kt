package com.apollographql.apollo3

import android.os.Handler
import android.os.Looper
import com.apollographql.apollo3.ApolloCallback
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException
import com.apollographql.apollo3.exception.ApolloParseException

/**
 *
 * Android wrapper for [ApolloCall.Callback] to be operated on specified [Handler]
 *
 * **NOTE:** [.onHttpError] will be called on the background thread if provided handler is
 * attached to the main looper. This behaviour is intentional as [ApolloHttpException] internally has a reference
 * to raw [okhttp3.Response] that must be closed on the background, otherwise it throws [ ] exception.
 */
class ApolloCallback<D: Operation.Data>(callback: ApolloCall.Callback<D>, handler: Handler) : ApolloCall.Callback<D>() {
  val delegate: ApolloCall.Callback<D>
  private val handler: Handler
  override fun onResponse(response: ApolloResponse<D>) {
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
    fun <D: Operation.Data> wrap(callback: ApolloCall.Callback<D>, handler: Handler): ApolloCallback<D> {
      return ApolloCallback(callback, handler)
    }
  }

  /**
   * @param callback original callback to delegates calls
   * @param handler  the callback will be run on the thread to which this handler is attached
   */
  init {
    delegate = callback
    this.handler = handler
  }
}
