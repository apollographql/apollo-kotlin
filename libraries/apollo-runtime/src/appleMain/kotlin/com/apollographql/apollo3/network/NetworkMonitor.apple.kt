package com.apollographql.apollo3.network

import com.apollographql.apollo3.annotations.ApolloExperimental

@ApolloExperimental
fun NetworkMonitor(): NetworkMonitor = DefaultNetworkMonitor { AppleNetworkObserver() }