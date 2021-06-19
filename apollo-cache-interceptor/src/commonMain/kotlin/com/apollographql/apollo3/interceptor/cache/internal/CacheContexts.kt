package com.apollographql.apollo3.interceptor.cache.internal

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.RequestContext
import com.apollographql.apollo3.api.ResponseContext
import com.apollographql.apollo3.interceptor.cache.FetchPolicy

internal class CacheContext(
    val fetchPolicy: FetchPolicy,
    val refetchPolicy: FetchPolicy? = null,
    val optimisticData: Any? = null,
    val flags: Int = 0
): RequestContext(Key) {

  fun copy(
      fetchPolicy: FetchPolicy = this.fetchPolicy,
      refetchPolicy: FetchPolicy? = this.refetchPolicy,
      optimisticData: Any? = this.optimisticData,
      flags: Int = this.flags
  ): CacheContext {
    return CacheContext(
        fetchPolicy,
        refetchPolicy,
        optimisticData,
        flags
    )
  }
  companion object Key : ExecutionContext.Key<CacheContext>
}

internal fun <D: Operation.Data> DefaultCacheContext(operation: Operation<D>): CacheContext {
  return CacheContext(defaultFetchPolicy(operation))
}

fun <D: Operation.Data> defaultFetchPolicy( operation: Operation<D>) = if (operation is Query) {
  FetchPolicy.CacheFirst
} else {
  FetchPolicy.NetworkOnly
}

internal class CacheOutput(
    val isFromCache: Boolean
) : ResponseContext(CacheOutput) {
  companion object Key : ExecutionContext.Key<CacheOutput>
}
