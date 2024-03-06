package com.apollographql.apollo3.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import okio.Closeable

internal interface PlatformNetworkMonitor: Closeable {
  fun setListener(listener: Listener)

  interface Listener {
    fun networkChanged(isOnline: Boolean)
  }
}

internal expect fun platformNetworkMonitor(): PlatformNetworkMonitor?

interface NetworkMonitor: Closeable {
  val isOnline: Boolean
  suspend fun waitForNetwork()
}

internal class DefaultNetworkMonitor(private val platformNetworkMonitor: PlatformNetworkMonitor): NetworkMonitor, PlatformNetworkMonitor.Listener {
  private val _isOnline = MutableStateFlow(false)
  init {
    platformNetworkMonitor.setListener(this)
  }

  override val isOnline: Boolean
    get() = _isOnline.value

  override suspend fun waitForNetwork() {
    _isOnline.takeWhile { !it }.collect()
  }

  override fun close() {
    platformNetworkMonitor.close()
  }

  override fun networkChanged(isOnline: Boolean) {
    this._isOnline.value = isOnline
  }
}
