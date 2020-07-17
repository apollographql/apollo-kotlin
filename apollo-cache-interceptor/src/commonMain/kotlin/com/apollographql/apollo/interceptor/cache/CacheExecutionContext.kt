package com.apollographql.apollo.interceptor.cache

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response

@ApolloExperimental
data class CacheExecutionContext(
    val fromCache: Boolean
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<CacheExecutionContext>
}

fun <D: Operation.Data> Response<D>.cacheContext(): CacheExecutionContext = executionContext[CacheExecutionContext] ?: throw IllegalStateException("ApolloGraphQL: no CacheExecutionContext")
