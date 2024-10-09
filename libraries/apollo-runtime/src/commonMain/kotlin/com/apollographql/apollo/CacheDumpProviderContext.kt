package com.apollographql.apollo

import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.ExecutionContext

/**
 * Cache dump provider, which can be set by cache implementations.
 */
@ApolloInternal
class CacheDumpProviderContext(
    val cacheDumpProvider: () -> Map<String, Map<String, Pair<Int, Map<String, Any?>>>>,
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  @ApolloInternal
  companion object Key : ExecutionContext.Key<CacheDumpProviderContext>
}
