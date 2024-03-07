package com.apollographql.apollo3.network

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import okio.Closeable
import kotlin.js.JsName

@ApolloExperimental
interface NetworkMonitor: Closeable {
  val isOnline: Boolean
  suspend fun waitForNetwork()
}

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