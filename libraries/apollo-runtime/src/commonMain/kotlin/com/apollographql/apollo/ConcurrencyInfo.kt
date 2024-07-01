package com.apollographql.apollo

import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/**
 * Gives access to [ApolloClient] dispatcher and scope. This is used by the normalized cache to execute cache writes in the
 * background.
 *
 * @param dispatcher the [ApolloClient] dispatcher
 * @param coroutineScope a scope bound to this [ApolloClient]. Canceled when [ApolloClient.close] is called.
 *
 * @see [ApolloClient.Builder.dispatcher]
 */
@ApolloExperimental
class ConcurrencyInfo(
    val dispatcher: CoroutineDispatcher,
    val coroutineScope: CoroutineScope,
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<ConcurrencyInfo>
}



