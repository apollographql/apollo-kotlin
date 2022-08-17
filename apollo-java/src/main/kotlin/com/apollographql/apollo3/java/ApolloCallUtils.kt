@file:JvmName("ApolloCallUtils")

package com.apollographql.apollo3.java

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.normalized.executeCacheAndNetwork
import com.apollographql.apollo3.cache.normalized.watch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.apollographql.apollo3.ApolloCall as ApolloKotlinCall

private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun <D : Operation.Data> ApolloKotlinCall<D>.execute(callback: ApolloCallback<D>): ApolloCall.Subscription {
  return launchAndCollectToCallback(toFlow(), callback)
}

fun <D : Operation.Data> ApolloKotlinCall<D>.executeBlocking(): ApolloResponse<D> {
  return runBlocking { execute() }
}

fun <D : Query.Data> ApolloKotlinCall<D>.watch(
    fetchThrows: Boolean,
    refetchThrows: Boolean,
    callback: ApolloCallback<D>,
): ApolloCall.Subscription {
  return launchAndCollectToCallback(watch(fetchThrows, refetchThrows), callback)
}

fun <D : Query.Data> ApolloKotlinCall<D>.watch(
    callback: ApolloCallback<D>,
): ApolloCall.Subscription {
  return launchAndCollectToCallback(watch(), callback)
}

fun <D : Query.Data> ApolloKotlinCall<D>.watch(
    data: D?,
    retryWhen: RetryPredicate,
    callback: ApolloCallback<D>,
): ApolloCall.Subscription {
  return launchAndCollectToCallback(watch(data, retryWhen = { cause, attempt -> retryWhen.shouldRetry(cause, attempt) }), callback)
}

fun <D : Query.Data> ApolloKotlinCall<D>.watch(
    data: D?,
    callback: ApolloCallback<D>,
): ApolloCall.Subscription {
  return launchAndCollectToCallback(watch(data), callback)
}

fun <D : Query.Data> ApolloKotlinCall<D>.executeCacheAndNetwork(
    callback: ApolloCallback<D>,
): ApolloCall.Subscription {
  return launchAndCollectToCallback(executeCacheAndNetwork(), callback)
}

private fun <D : Operation.Data> launchAndCollectToCallback(
    flow: Flow<ApolloResponse<D>>,
    callback: ApolloCallback<D>,
): ApolloCall.Subscription {
  return launch {
    flow.collectToCallback(callback)
  }
}

private fun launch(block: suspend () -> Unit): ApolloCall.Subscription {
  val job = coroutineScope.launch {
    block()
  }
  return ApolloCall.Subscription { job.cancel() }
}

private suspend fun <D : Operation.Data> Flow<ApolloResponse<D>>.collectToCallback(callback: ApolloCallback<D>) {
  catch { throwable ->
    callback.onFailure(throwable)
  }
  collect {
    callback.onResponse(it)
  }
}
