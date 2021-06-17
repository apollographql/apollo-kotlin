package com.apollographql.apollo3.interceptor.cache.internal

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.RequestContext
import com.apollographql.apollo3.api.ResponseContext
import com.apollographql.apollo3.interceptor.cache.FetchPolicy

internal data class FetchPolicyContext(
    val fetchPolicy: FetchPolicy?,
): RequestContext(Key) {
  companion object Key : ExecutionContext.Key<FetchPolicyContext>
}
internal data class RefetchPolicyContext(
    val refetchPolicy: FetchPolicy?,
): RequestContext(Key) {
  companion object Key : ExecutionContext.Key<RefetchPolicyContext>
}

internal data class OptimisticUpdates<D>(
    val data: D
) : RequestContext(OptimisticUpdates) {
  companion object Key : ExecutionContext.Key<OptimisticUpdates<*>>
}

internal data class CacheOutput(
    val isFromCache: Boolean
) : ResponseContext(CacheOutput) {
  companion object Key : ExecutionContext.Key<CacheOutput>
}