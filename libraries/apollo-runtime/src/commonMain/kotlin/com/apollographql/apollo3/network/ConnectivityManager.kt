package com.apollographql.apollo3.network

import okio.Closeable

internal interface PlatformConnectivityManager: Closeable {
  fun setListener(listener: Listener)

  interface Listener {
    fun networkChanged(isOnline: Boolean)
  }
}

internal expect fun platformConnectivityManager(): PlatformConnectivityManager?
