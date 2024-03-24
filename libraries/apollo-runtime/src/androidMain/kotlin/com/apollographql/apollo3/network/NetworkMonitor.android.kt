@file:JvmMultifileClass
@file:JvmName("NetworkMonitorKt")

package com.apollographql.apollo3.network

import android.content.Context
import com.apollographql.apollo3.annotations.ApolloExperimental

/**
 * Returns a new [NetworkMonitor] for the given [Context]
 *
 * @return the network monitor or `null` if the [ConnectivityManager](https://developer.android.com/reference/android/net/ConnectivityManager) cannot be found
 * or if the [ACCESS_NETWORK_STATE](https://developer.android.com/reference/android/Manifest.permission#ACCESS_NETWORK_STATE) permission is missing.
 */
@ApolloExperimental
fun NetworkMonitor(context: Context): NetworkMonitor? = platformConnectivityManager(context)?.toNetworkMonitor()