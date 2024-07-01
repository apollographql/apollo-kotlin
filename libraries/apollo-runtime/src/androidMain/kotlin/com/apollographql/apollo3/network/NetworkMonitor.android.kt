@file:JvmMultifileClass
@file:JvmName("NetworkMonitorKt")

package com.apollographql.apollo.network

import android.content.Context
import com.apollographql.apollo.annotations.ApolloExperimental

/**
 * Returns a new [NetworkMonitor] for the given [Context]
 *
 * In order to work correctly, this requires:
 * - minSdk >= 23
 * - declaring the [ACCESS_NETWORK_STATE](https://developer.android.com/reference/android/Manifest.permission#ACCESS_NETWORK_STATE) permission in your Manifest
 *
 * If one of these conditions is not satisfied, the returned [NetworkMonitor] will behave as if the device were always online.
 */
@ApolloExperimental
fun NetworkMonitor(context: Context): NetworkMonitor = DefaultNetworkMonitor { networkObserver(context) }