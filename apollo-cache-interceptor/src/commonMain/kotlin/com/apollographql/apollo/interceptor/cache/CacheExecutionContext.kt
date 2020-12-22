package com.apollographql.apollo.interceptor.cache

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.NormalizedCache

@ApolloExperimental
data class CacheExecutionContext(
    val fromCache: Boolean
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<CacheExecutionContext>
}

@ApolloExperimental
val <D: Operation.Data> Response<D>.fromCache
  get() =  executionContext[CacheExecutionContext]?.fromCache ?: throw IllegalStateException("ApolloGraphQL: no CacheExecutionContext")

@ApolloExperimental
fun ApolloClient.cloneWithCache(normalizedCache: NormalizedCache) {
  return
}