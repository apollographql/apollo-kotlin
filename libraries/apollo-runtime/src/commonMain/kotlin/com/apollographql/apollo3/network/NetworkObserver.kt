package com.apollographql.apollo.network

import okio.Closeable

internal interface NetworkObserver: Closeable {
  /**
   * Sets the listener
   *
   * Implementation must call [listener] shortly after [setListener] returns to let the callers know about the initial state.
   */
  fun setListener(listener: Listener)

  interface Listener {
    fun networkChanged(isOnline: Boolean)
  }
}

internal val NoOpNetworkObserver = object : NetworkObserver {
  override fun setListener(listener: NetworkObserver.Listener) {
    listener.networkChanged(true)
  }

  override fun close() {}
}