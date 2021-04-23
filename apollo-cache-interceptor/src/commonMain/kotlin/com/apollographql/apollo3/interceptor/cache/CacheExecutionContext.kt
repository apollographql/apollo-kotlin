package com.apollographql.apollo3.interceptor.cache

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.RequestContext
import com.apollographql.apollo3.api.ResponseContext
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.exception.ApolloCompositeException
import com.apollographql.apollo3.exception.ApolloException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class FetchPolicy {
  /**
   * Try cache first, then network
   *
   * This is the default behaviour
   */
  CacheFirst,

  /**
   * Only try cache
   */
  CacheOnly,

  /**
   * Try network first, then cache
   */
  NetworkFirst,

  /**
   * Only try network
   */
  NetworkOnly,
}

internal data class CacheInput(
    val fetchPolicy: FetchPolicy?,
    val watch: Boolean
): RequestContext(Key) {
  companion object Key : ExecutionContext.Key<CacheInput>
}

internal data class CacheOutput(
    val isFromCache: Boolean
) : ResponseContext(CacheOutput) {
  companion object Key : ExecutionContext.Key<CacheOutput>
}

val <D : Operation.Data> ApolloResponse<D>.isFromCache
  get() = executionContext[CacheOutput]?.isFromCache ?: false

fun ApolloClient.Builder.normalizedCache(store: ApolloStore): ApolloClient.Builder {
  return addInterceptor(
      ApolloCacheInterceptor(),
      store
  )
}

fun <D: Query.Data> ApolloRequest<D>.withFetchPolicy(fetchPolicy: FetchPolicy): ApolloRequest<D> {
  return withExecutionContext(CacheInput(fetchPolicy = fetchPolicy, watch = false))
}

fun <D : Query.Data> ApolloClient.queryCacheAndNetwork(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
  return flow {
    val cacheResult = kotlin.runCatching {
      query(queryRequest.withFetchPolicy(FetchPolicy.CacheOnly))
    }
    val cacheResponse = cacheResult.getOrNull()
    if (cacheResponse != null) {
      emit(cacheResponse)
    }
    val networkResult = kotlin.runCatching {
      query(queryRequest.withFetchPolicy(FetchPolicy.NetworkOnly))
    }
    val networkResponse = networkResult.getOrNull()
    if (networkResponse != null) {
      emit(networkResponse)
    }

    if (cacheResponse == null && networkResponse == null) {
      throw ApolloCompositeException(
          cacheResult.exceptionOrNull() as ApolloException,
          networkResult.exceptionOrNull() as ApolloException
      )
    }
  }
}

fun <D : Query.Data> ApolloClient.watch(query: Query<D>): Flow<ApolloResponse<D>> {
  val queryRequest = ApolloRequest(query).withExecutionContext(
      CacheInput(fetchPolicy = null, watch = true)
  )
  return queryAsFlow(queryRequest)
}


fun <D : Query.Data> ApolloClient.watch(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
  val cacheInput = queryRequest.executionContext[CacheInput]?.copy(
      watch = true
  ) ?: CacheInput(fetchPolicy = null, watch = true)

  return queryAsFlow(queryRequest.withExecutionContext(cacheInput))
}
