package com.apollographql.apollo3.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES.M
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.apollographql.apollo3.ApolloInitializer
import java.lang.ref.WeakReference

/**
 * isPermissionGranted, WeakReferences and other things here inspired by Coil [NetworkObserver](https://github.com/coil-kt/coil/blob/24375db1775fb46f0e184501646cd9e150185608/coil-core/src/androidMain/kotlin/coil3/util/NetworkObserver.kt)
 */
internal fun Context.isPermissionGranted(permission: String): Boolean {
  return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

actual fun NetworkMonitor(): NetworkMonitor? {
  return if (VERSION.SDK_INT >= M) {
    val connectivityManager = ApolloInitializer.context.getSystemService(ConnectivityManager::class.java)
    if (connectivityManager == null || !ApolloInitializer.context.isPermissionGranted(Manifest.permission.ACCESS_NETWORK_STATE)) {
      println("Cannot get ConnectivityManager")
      return null
    }

    DefaultNetworkMonitor(connectivityManager)
  } else {
    null
  }
}

@RequiresApi(M)
@SuppressLint("MissingPermission")
internal class DefaultNetworkMonitor(private val connectivityManager: ConnectivityManager) : NetworkMonitor {
  private val listeners = mutableListOf<WeakReference<NetworkMonitor.Listener>>()

  private val networkCallback: NetworkCallback = object : NetworkCallback() {
    override fun onAvailable(network: Network) = onConnectivityChange(true)
    override fun onLost(network: Network) = onConnectivityChange(false)
  }

  init {
    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    connectivityManager.registerNetworkCallback(request, networkCallback)
  }

  private fun onConnectivityChange(isOnline: Boolean) = synchronized(this) {
    val iterator = listeners.iterator()
    while (iterator.hasNext()) {
      when (val listener = iterator.next().get()) {
        null -> iterator.remove()
        else -> listener.networkChanged(isOnline)
      }
    }
    if (listeners.isEmpty()) {
      connectivityManager.unregisterNetworkCallback(networkCallback)
    }
  }

  override fun registerListener(listener: NetworkMonitor.Listener): Unit = synchronized(this) {
    listeners.add(WeakReference(listener))
  }

  override fun unregisterListener(listener: NetworkMonitor.Listener): Unit = synchronized(this) {
    val iterator = listeners.iterator()
    while (iterator.hasNext()) {
      when (iterator.next().get()) {
        null -> iterator.remove()
        listener -> iterator.remove()
      }
    }
  }
}