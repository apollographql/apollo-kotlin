@file:JvmName("ApolloClientAdapter")

package com.apollographql.apollo3.java.internal

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.cache.normalized.store
import com.apollographql.apollo3.java.RetryPredicate
import com.apollographql.apollo3.java.cache.normalized.ApolloStore
import com.apollographql.apollo3.java.cache.normalized.internal.ApolloStoreAdapter
import com.apollographql.apollo3.ApolloClient as ApolloKotlinClient

internal fun ApolloKotlinClient.Builder.webSocketReopenWhen(reopenWhen: RetryPredicate): ApolloKotlinClient.Builder {
  return apply { webSocketReopenWhen { cause, attempt -> reopenWhen.shouldRetry(cause, attempt) } }
}

fun ApolloClient.Builder.store(store: ApolloStore, writeToCacheAsynchronously: Boolean): ApolloClient.Builder {
  return apply {
    store((store as ApolloStoreAdapter).apolloKotlinStore, writeToCacheAsynchronously)
  }
}

fun ApolloClient.Builder.store(store: ApolloStore): ApolloClient.Builder {
  return apply {
    store((store as ApolloStoreAdapter).apolloKotlinStore)
  }
}
