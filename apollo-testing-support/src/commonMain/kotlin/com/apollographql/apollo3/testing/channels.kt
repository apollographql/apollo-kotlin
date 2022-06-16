package com.apollographql.apollo3.testing

import com.apollographql.apollo3.annotations.ApolloExperimental
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeout

@ApolloExperimental
suspend fun <T> Channel<T>.receiveOrTimeout(timeoutMillis: Long = 500) = withTimeout(timeoutMillis) {
  receive()
}
