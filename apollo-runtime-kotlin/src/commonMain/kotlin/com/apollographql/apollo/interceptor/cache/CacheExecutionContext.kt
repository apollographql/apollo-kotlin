package com.apollographql.apollo.cache

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext

@ApolloExperimental
data class CacheExecutionContext(
    val fromCache: Boolean
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<CacheExecutionContext>
}

