@file:JvmMultifileClass
@file:JvmName("NetworkMonitorKt")
package com.apollographql.apollo3.network

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import okio.Closeable
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Monitors the network state.
 * A [NetworkMonitor] is used to retry requests when network is available.
 */
@ApolloExperimental
interface NetworkMonitor: Closeable {
  /**
   * The current state of the network
   */
  val isOnline: Boolean

  /**
   * Waits until [isOnline] is true
   */
  suspend fun waitForNetwork()
}

val NoOpNetworkMonitor = object : NetworkMonitor {
  override val isOnline: Boolean
    get() = true
  override suspend fun waitForNetwork() {}
  override fun close() {}
}

internal class DefaultNetworkMonitor(private val platformConnectivityManager: PlatformConnectivityManager): NetworkMonitor, PlatformConnectivityManager.Listener {
  private val _isOnline = MutableStateFlow(false)
  init {
    platformConnectivityManager.setListener(this)
  }

  override val isOnline: Boolean
    get() = _isOnline.value

  override suspend fun waitForNetwork() {
    _isOnline.takeWhile { !it }.collect()
  }

  override fun close() {
    platformConnectivityManager.close()
  }

  override fun networkChanged(isOnline: Boolean) {
    this._isOnline.value = isOnline
  }
}