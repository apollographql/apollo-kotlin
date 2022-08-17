@file:JvmName("ApolloCallUtils")

package com.apollographql.apollo3.java

import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Operation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.Closeable

private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

fun <D : Operation.Data> com.apollographql.apollo3.ApolloCall<D>.subscribe(callback: ApolloCallback<D>): Closeable {
  val job = coroutineScope.launch {
    toFlow()
        .catch { throwable ->
          callback.onFailure(throwable)
        }
        .collect {
          callback.onResponse(it)
        }
  }
  return Closeable { job.cancel() }
}

fun <D : Operation.Data> com.apollographql.apollo3.ApolloCall<D>.execute(callback: ApolloCallback<D>) {
  coroutineScope.launch {
    try {
      callback.onResponse(execute())
    } catch (throwable: Throwable) {
      callback.onFailure(throwable)
    }
  }
}

fun <D : Operation.Data> com.apollographql.apollo3.ApolloCall<D>.executeBlocking(): ApolloResponse<D> {
  return runBlocking { execute() }
}
