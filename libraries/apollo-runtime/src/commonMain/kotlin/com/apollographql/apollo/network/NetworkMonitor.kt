@file:JvmMultifileClass
@file:JvmName("NetworkMonitorKt")

package com.apollographql.apollo.network

import com.apollographql.apollo.annotations.ApolloExperimental
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.takeWhile
import okio.Closeable
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

/**
 * Monitors the network state.
 * A [NetworkMonitor] is used to retry requests when network is available.
 */
@ApolloExperimental
interface NetworkMonitor : Closeable {
  /**
   * The current state of the network
   */
  suspend fun isOnline(): Boolean

  /**
   * Waits until [isOnline] is true
   */
  suspend fun waitForNetwork()
}

internal class DefaultNetworkMonitor(private val networkObserverFactory: () -> NetworkObserver) : NetworkMonitor, NetworkObserver.Listener {
  private val _isOnline: MutableStateFlow<Boolean?> = MutableStateFlow(null)

  private val networkObserver by lazy {
    networkObserverFactory().also {
      it.setListener(this)
    }
  }

  override suspend fun isOnline(): Boolean {
    networkObserver
    return _isOnline.mapNotNull { it }.first()
  }

  override suspend fun waitForNetwork() {
    networkObserver
    _isOnline.takeWhile { it != true }.collect()
  }

  override fun close() {
    networkObserver.close()
  }

  override fun networkChanged(isOnline: Boolean) {
    this._isOnline.value = isOnline
  }
}