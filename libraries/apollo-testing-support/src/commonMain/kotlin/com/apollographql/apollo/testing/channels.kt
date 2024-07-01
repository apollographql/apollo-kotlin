package com.apollographql.apollo.testing

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

@ApolloExperimental
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
suspend fun <T> Channel<T>.awaitElement(timeoutMillis: Long = 30000) = withTimeout(timeoutMillis) {
  receive()
}

@ApolloExperimental
@ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
@Deprecated("This function is not Apollo specific and will be removed in a future version. Copy/paste it in your codebase if you need it")
suspend fun <T> Channel<T>.assertNoElement(timeoutMillis: Long = 300): Unit {
  try {
    withTimeout(timeoutMillis) {
      receive()
    }
    error("An item was unexpectedly received")
  } catch (_: TimeoutCancellationException) {
    // nothing
  }
}
