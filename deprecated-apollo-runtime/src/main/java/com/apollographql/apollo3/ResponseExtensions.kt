package com.apollographql.apollo3

import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.ResponseContext

class CacheInfo(val isFromCache: Boolean): ResponseContext(CacheInfo) {
  companion object Key: ExecutionContext.Key<CacheInfo>
}

val <D: Operation.Data> ApolloResponse<D>.isFromCache: Boolean
  get() = executionContext[CacheInfo]?.isFromCache ?: false // Default to false because this was the previous behaviour

fun <D: Operation.Data> ApolloResponse<D>.withCacheInfo(isFromCache: Boolean) = copy(
    executionContext = executionContext + CacheInfo(isFromCache)
)