@file:JvmName("ApolloClientUtils")

package com.apollographql.apollo3.java

import com.apollographql.apollo3.ApolloClient as ApolloKotlinClient

fun ApolloKotlinClient.Builder.webSocketReopenWhen(reopenWhen: RetryPredicate): ApolloKotlinClient.Builder {
  return apply { webSocketReopenWhen { cause, attempt -> reopenWhen.shouldRetry(cause, attempt) } }
}
