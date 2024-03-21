package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

@ApolloExperimental
suspend fun <T> Channel<T>.awaitElement(timeoutMillis: Long = 30000) = withTimeout(timeoutMillis) {
  receive()
}

@ApolloExperimental
suspend fun <T> Channel<T>.assertNoElement(timeoutMillis: Long = 300): Unit {
  try {
    withTimeout(timeoutMillis) {
      receive()
    }
    error("An item was received and no item was ")
  } catch (_: TimeoutCancellationException) {
    // nothing
  }
}
