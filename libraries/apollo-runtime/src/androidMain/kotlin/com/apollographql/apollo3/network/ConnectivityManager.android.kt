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


@RequiresApi(M)
@SuppressLint("MissingPermission")
internal class AndroidPlatformConnectivityManager(private val connectivityManager: ConnectivityManager) : PlatformConnectivityManager {
  private var listener: WeakReference<PlatformConnectivityManager.Listener>? = null

  private val networkCallback: NetworkCallback = object : NetworkCallback() {
    override fun onAvailable(network: Network) = onConnectivityChange(true)
    override fun onLost(network: Network) = onConnectivityChange(false)
  }

  private fun onConnectivityChange(isOnline: Boolean) {
    val listener = listener!!.get()
    if (listener == null) {
      close()
    } else {
      listener.networkChanged(isOnline)
    }
  }

  override fun setListener(listener: PlatformConnectivityManager.Listener) {
    check(this.listener == null) {
      "There can be only one listener"
    }
    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    this.listener = WeakReference(listener)

    connectivityManager.registerNetworkCallback(request, networkCallback)
  }

  override fun close() {
    connectivityManager.unregisterNetworkCallback(networkCallback)
    listener = null
  }
}

internal actual fun platformConnectivityManager(): PlatformConnectivityManager? {
  return if (VERSION.SDK_INT >= M) {
    val connectivityManager = ApolloInitializer.context?.getSystemService(ConnectivityManager::class.java)
    val hasPermission = ApolloInitializer.context?.isPermissionGranted(Manifest.permission.ACCESS_NETWORK_STATE) ?: false
    if (connectivityManager == null || !hasPermission) {
      println("Cannot get ConnectivityManager")
      return null
    }

    AndroidPlatformConnectivityManager(connectivityManager)
  } else {
    null
  }
}