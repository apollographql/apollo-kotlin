package com.apollographql.apollo3

import android.os.Handler
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.exception.ApolloNetworkException

/**
 * Android wrapper for [ApolloPrefetch.Callback] to be operated on specified [Handler]
 */
class ApolloPrefetchCallback(callback: ApolloPrefetch.Callback, handler: Handler) : ApolloPrefetch.Callback() {
  val delegate: ApolloPrefetch.Callback
  private val handler: Handler
  override fun onSuccess() {
    handler.post { delegate.onSuccess() }
  }

  override fun onFailure(e: ApolloException) {
    handler.post { delegate.onFailure(e) }
  }

  override fun onHttpError(e: ApolloHttpException) {
    handler.post { delegate.onHttpError(e) }
  }

  override fun onNetworkError(e: ApolloNetworkException) {
    handler.post { delegate.onNetworkError(e) }
  }

  companion object {
    /**
     * Wraps `callback` to be be operated on specified `handler`
     *
     * @param callback original callback to delegates calls
     * @param handler  the callback will be run on the thread to which this handler is attached
     */
    fun <T> wrap(callback: ApolloPrefetch.Callback, handler: Handler): ApolloPrefetchCallback {
      return ApolloPrefetchCallback(callback, handler)
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