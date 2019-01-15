@file:Suppress("unused")

package com.apollographql.apollo.livedata

import android.arch.lifecycle.LiveData
import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloPrefetch
import com.apollographql.apollo.ApolloQueryWatcher
import com.apollographql.apollo.ApolloSubscriptionCall
import com.apollographql.apollo.cache.normalized.ApolloStoreOperation

/**
 * The ApolloExtension class provides extensions for converting ApolloCall, ApolloPrefetch and ApolloWatcher types to LiveData
 * sources.
 */

/**
 * Converts an [ApolloCall] to a LiveData.
 */
fun <T> ApolloCall<T>.toLiveData(): LiveData<ApolloLiveDataResponse<T>> {
  return LiveDataApollo.from(this)
}

/**
 * Converts an [ApolloQueryWatcher] to a LiveData.
 */
fun <T> ApolloQueryWatcher<T>.toLiveData(): LiveData<ApolloLiveDataResponse<T>> {
  return LiveDataApollo.from(this)
}

/**
 * Converts an [ApolloPrefetch] to a LiveData.
 */
fun <T> ApolloPrefetch.toLiveData(): LiveData<ApolloLiveDataResponse<T>> {
  return LiveDataApollo.from(this)
}

/**
 * Converts an [ApolloSubscriptionCall] to a LiveData.
 */
fun <T> ApolloSubscriptionCall<T>.toLiveData(): LiveData<ApolloLiveDataResponse<T>> {
  return LiveDataApollo.from(this)
}

/**
 * Converts an [ApolloStoreOperation] to a LiveData.
 */
fun <T> ApolloStoreOperation<T>.toLiveData(): LiveData<T?> {
  return LiveDataApollo.from(this)
}
