package com.apollographql.apollo.network

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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import java.lang.ref.WeakReference
/**
 * isPermissionGranted, WeakReferences and other things here inspired by Coil [NetworkObserver](https://github.com/coil-kt/coil/blob/24375db1775fb46f0e184501646cd9e150185608/coil-core/src/androidMain/kotlin/coil3/util/NetworkObserver.kt)
 */
internal fun Context.isPermissionGranted(permission: String): Boolean {
  return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

@RequiresApi(M)
@SuppressLint("MissingPermission")
internal class AndroidNetworkObserver(private val connectivityManager: ConnectivityManager) : NetworkObserver {
  private var listener: WeakReference<NetworkObserver.Listener>? = null

  /**
   * Not locked because I'm assuming the [NetworkCallback] is always called on the same thread
   * and the thread safety comes from [DefaultNetworkMonitor._isOnline]
   */
  private var onlineNetworks = mutableSetOf<Long>()

  private val networkCallback: NetworkCallback = object : NetworkCallback() {
    override fun onAvailable(network: Network) {
      onlineNetworks.add(network.networkHandle)
      onConnectivityChange(onlineNetworks.isNotEmpty())
    }

    override fun onLost(network: Network) {
      onlineNetworks.remove(network.networkHandle)
      onConnectivityChange(onlineNetworks.isNotEmpty())
    }
  }

  private fun onConnectivityChange(isOnline: Boolean) {
    val listener = listener!!.get()
    if (listener == null) {
      close()
    } else {
      listener.networkChanged(isOnline)
    }
  }

  override fun setListener(listener: NetworkObserver.Listener) {
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

private val TAG = "Apollo"

internal fun networkObserver(context: Context): NetworkObserver {
  if (VERSION.SDK_INT < M) {
    Log.w(TAG, "network monitoring requires minSdk of 23 or more")
    return NoOpNetworkObserver
  }
  val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
  if (connectivityManager !is ConnectivityManager) {
    Log.w(TAG, "Cannot get ConnectivityManager")
    return NoOpNetworkObserver
  }
  val hasPermission = context.isPermissionGranted(Manifest.permission.ACCESS_NETWORK_STATE)
  if (!hasPermission) {
    Log.w(TAG, "No ACCESS_NETWORK_STATE")
    return NoOpNetworkObserver
  }

  return AndroidNetworkObserver(connectivityManager)
}
