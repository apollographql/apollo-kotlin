package com.apollographql.apollo.network

import com.apollographql.apollo.annotations.ApolloExperimental

@ApolloExperimental
fun NetworkMonitor(): NetworkMonitor = DefaultNetworkMonitor { AppleNetworkObserver() }