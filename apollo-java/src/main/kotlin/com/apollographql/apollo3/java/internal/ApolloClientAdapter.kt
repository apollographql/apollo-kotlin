@file:JvmName("ApolloClientAdapter")

package com.apollographql.apollo3.java.internal

import com.apollographql.apollo3.java.RetryPredicate
import com.apollographql.apollo3.ApolloClient as ApolloKotlinClient

internal fun ApolloKotlinClient.Builder.webSocketReopenWhen(reopenWhen: RetryPredicate): ApolloKotlinClient.Builder {
  return apply { webSocketReopenWhen { cause, attempt -> reopenWhen.shouldRetry(cause, attempt) } }
}
