package com.apollographql.apollo3.testing

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

suspend fun <T> Channel<T>.receiveOrTimeout(timeoutMillis: Long = 500) = withTimeout(timeoutMillis) {
  receive()
}
