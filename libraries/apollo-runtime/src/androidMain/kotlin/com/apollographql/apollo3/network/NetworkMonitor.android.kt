@file:JvmMultifileClass
@file:JvmName("NetworkMonitorKt")

package com.apollographql.apollo3.network

import android.content.Context
import com.apollographql.apollo3.annotations.ApolloExperimental

/**
 * Returns a new [NetworkMonitor] for the given [Context]
 *
 * Use this function in contexts where androidx.startup is not available
 */
@ApolloExperimental
fun NetworkMonitor(context: Context): NetworkMonitor? = platformConnectivityManager(context)?.let { DefaultNetworkMonitor(it) }