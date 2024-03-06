package com.apollographql.apollo3.network

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface NetworkMonitor {
  fun registerListener(listener: Listener)

  fun unregisterListener(listener: Listener)

  interface Listener {
    fun networkChanged(isOnline: Boolean)
  }
}

expect fun NetworkMonitor(): NetworkMonitor?

suspend fun NetworkMonitor.waitForNetwork() = suspendCancellableCoroutine { continuation ->
  val listener = object : NetworkMonitor.Listener {
    override fun networkChanged(isOnline: Boolean) {
      if (isOnline) {
        continuation.resume(Unit)
        unregisterListener(this)
      }
    }
  }

  registerListener(listener)
  continuation.invokeOnCancellation { unregisterListener(listener) }
}