package com.apollographql.apollo.interceptor.cache

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.cache.normalized.NormalizedCache
import com.apollographql.apollo.interceptor.ApolloQueryRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.jvm.JvmField

@ApolloExperimental
data class CacheResponseExecutionContext(
    val fromCache: Boolean
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<CacheResponseExecutionContext>
}

enum class NormalizedCachePolicy {
  CACHE_ONLY,
  NETWORK_ONLY,
  CACHE_FIRST,
  NETWORK_FIRST,
  CACHE_AND_NETWORK
}

@ApolloExperimental
data class CacheRequestExecutionContext(
    val policy: NormalizedCachePolicy
) : ExecutionContext.Element {
  override val key: ExecutionContext.Key<*> = Key

  companion object Key : ExecutionContext.Key<CacheRequestExecutionContext>
}

@ApolloExperimental
fun <D : Operation.Data> ApolloQueryRequest.Builder<D>.normalizedCachePolicy(policy: NormalizedCachePolicy) = apply {
  addExecutionContext(CacheRequestExecutionContext(policy))
}

@ApolloExperimental
val <D : Operation.Data> Response<D>.fromCache
  get() = executionContext[CacheResponseExecutionContext]?.fromCache ?: throw IllegalStateException("ApolloGraphQL: no CacheExecutionContext")


@ExperimentalCoroutinesApi
@ApolloExperimental
fun ApolloClient.Builder.normalizedCache(normalizedCache: NormalizedCache): ApolloClient.Builder {
  return addInterceptor(ApolloCacheInterceptor(ApolloStore(normalizedCache)))
}