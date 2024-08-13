@file:JvmMultifileClass
@file:JvmName("NetworkMonitorKt")

package com.apollographql.apollo.network

import com.apollographql.apollo.annotations.ApolloExperimental
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
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
interface NetworkMonitor : Closeable {
  /**
   * Emits the current network state. May emit null during initialization
   * when the current state is not known yet.
   */
  val isOnline: StateFlow<Boolean?>
}

internal suspend fun NetworkMonitor.waitForNetwork() {
  isOnline.takeWhile { it != true }.collect()
}

/**
 * @param networkObserverFactory a factory for a [NetworkObserver]. [networkObserverFactory] is called from a
 * background thread.
 */
internal class DefaultNetworkMonitor(private val networkObserverFactory: () -> NetworkObserver) : NetworkMonitor, NetworkObserver.Listener {
  private val _isOnline: MutableStateFlow<Boolean?> = MutableStateFlow(null)

  override val isOnline: StateFlow<Boolean?>
    get() {
      networkObserver
      return _isOnline.asStateFlow()
    }

  private val networkObserver by lazy {
    networkObserverFactory().also {
      it.setListener(this)
    }
  }

  override fun close() {
    networkObserver.close()
  }

  override fun networkChanged(isOnline: Boolean) {
    this._isOnline.value = isOnline
  }
}