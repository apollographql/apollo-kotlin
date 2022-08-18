@file:JvmName("ApolloCallAdapter")

package com.apollographql.apollo3.java.internal

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.executeCacheAndNetwork
import com.apollographql.apollo3.cache.normalized.watch
import com.apollographql.apollo3.java.ApolloCallback
import com.apollographql.apollo3.java.RetryPredicate
import com.apollographql.apollo3.java.Subscription
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import com.apollographql.apollo3.ApolloCall as ApolloKotlinCall

internal fun <D : Operation.Data> ApolloKotlinCall<D>.execute(callback: ApolloCallback<D>): Subscription {
  return launchAndCollectToCallback(toFlow(), callback)
}

internal fun <D : Operation.Data> ApolloKotlinCall<D>.executeBlocking(): ApolloResponse<D> {
  return runBlocking { execute() }
}

internal fun <D : Query.Data> ApolloKotlinCall<D>.watch(
    fetchThrows: Boolean,
    refetchThrows: Boolean,
    callback: ApolloCallback<D>,
): Subscription {
  return launchAndCollectToCallback(watch(fetchThrows, refetchThrows), callback)
}

internal fun <D : Query.Data> ApolloKotlinCall<D>.watch(
    callback: ApolloCallback<D>,
): Subscription {
  return launchAndCollectToCallback(watch(), callback)
}

internal fun <D : Query.Data> ApolloKotlinCall<D>.watch(
    data: D?,
    retryWhen: RetryPredicate,
    callback: ApolloCallback<D>,
): Subscription {
  return launchAndCollectToCallback(watch(data, retryWhen = { cause, attempt -> retryWhen.shouldRetry(cause, attempt) }), callback)
}

internal fun <D : Query.Data> ApolloKotlinCall<D>.watch(
    data: D?,
    callback: ApolloCallback<D>,
): Subscription {
  return launchAndCollectToCallback(watch(data), callback)
}

internal fun <D : Query.Data> ApolloKotlinCall<D>.executeCacheAndNetwork(
    callback: ApolloCallback<D>,
): Subscription {
  return launchAndCollectToCallback(executeCacheAndNetwork(), callback)
}

private fun <D : Operation.Data> launchAndCollectToCallback(
    flow: Flow<ApolloResponse<D>>,
    callback: ApolloCallback<D>,
): Subscription {
  return launchToSubscription {
    flow.collectToCallback(callback)
  }
}

private suspend fun <D : Operation.Data> Flow<ApolloResponse<D>>.collectToCallback(callback: ApolloCallback<D>) {
  catch { throwable ->
    callback.onFailure(throwable)
  }
  collect {
    callback.onResponse(it)
  }
}
