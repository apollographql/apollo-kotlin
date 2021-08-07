package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.FetchPolicy

internal class CacheInput(
    val fetchPolicy: FetchPolicy,
    val refetchPolicy: FetchPolicy? = null,
    val optimisticData: Any? = null,
    val flags: Int = 0,
    val cacheHeaders: CacheHeaders = CacheHeaders.NONE,
    val writeToCacheAsynchronously: Boolean? = null
): ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  fun copy(
      fetchPolicy: FetchPolicy = this.fetchPolicy,
      refetchPolicy: FetchPolicy? = this.refetchPolicy,
      optimisticData: Any? = this.optimisticData,
      flags: Int = this.flags,
      cacheHeaders: CacheHeaders = this.cacheHeaders,
      writeToCacheAsynchronously: Boolean? = this.writeToCacheAsynchronously
  ): CacheInput {
    return CacheInput(
        fetchPolicy,
        refetchPolicy,
        optimisticData,
        flags,
        cacheHeaders,
        writeToCacheAsynchronously
    )
  }
  companion object Key : ExecutionContext.Key<CacheInput>
}

internal fun <D: Operation.Data> DefaultCacheInput(operation: Operation<D>): CacheInput {
  return CacheInput(defaultFetchPolicy(operation))
}

fun <D: Operation.Data> defaultFetchPolicy( operation: Operation<D>) = if (operation is Query) {
  FetchPolicy.CacheFirst
} else {
  FetchPolicy.NetworkOnly
}

internal class CacheOutput(
    val isFromCache: Boolean
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key

  companion object Key : ExecutionContext.Key<CacheOutput>
}

internal class StoreExecutionContext(val store: ApolloStore): ExecutionContext.Element {
  override val key: ExecutionContext.Key<*>
    get() = Key
  companion object Key: ExecutionContext.Key<StoreExecutionContext>
}