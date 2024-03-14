@file:JvmMultifileClass
@file:JvmName("NetworkMonitorKt")
package com.apollographql.apollo3.network

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import okio.Closeable
import kotlin.js.JsName
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

/**
 * Returns a default [NetworkMonitor] or null if no [NetworkMonitor] is available
 *
 * - On Android, uses [ConnectivityManager](https://developer.android.com/reference/android/net/ConnectivityManager)
 * - On iOS, uses [NWPathMonitor](https://developer.apple.com/documentation/network/nwpathmonitor)
 *
 * On Android, [NetworkMonitor] additionally requires the [ACCESS_NETWORK_STATE](https://developer.android.com/reference/android/Manifest.permission#ACCESS_NETWORK_STATE) permission
 */
@ApolloExperimental
@JsName("createNetworkMonitor")
fun NetworkMonitor(): NetworkMonitor? = platformConnectivityManager()?.let { DefaultNetworkMonitor(it) }

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