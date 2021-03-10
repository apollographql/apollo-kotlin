package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.Record
import com.benasher44.uuid.Uuid
import kotlin.reflect.KClass

/**
 * An alternative to RealApolloStore for when a no-operation cache is needed.
 */
internal class NoOpApolloStore : ApolloStore() {
  override fun subscribe(subscriber: RecordChangeSubscriber) {}
  override fun unsubscribe(subscriber: RecordChangeSubscriber) {}
  override fun publish(keys: Set<String>) {}
  override fun clearAll(): Boolean {
    return false
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return false
  }

  override fun remove(cacheKeys: List<CacheKey>, cascade: Boolean): Int {
    return 0
  }

  override fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      responseAdapterCache: ResponseAdapterCache,
      cacheHeaders: CacheHeaders,
      mode: ReadMode,
  ): D? {
    // This will be seen as a cache MISS and the request will go to the network.
    return null
  }

  override fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      responseAdapterCache: ResponseAdapterCache,
      cacheHeaders: CacheHeaders,
  ): D? {
    return null
  }

  override fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      responseAdapterCache: ResponseAdapterCache,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    return emptySet()
  }

  override fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      responseAdapterCache: ResponseAdapterCache,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    return emptySet()
  }


  override fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      responseAdapterCache: ResponseAdapterCache,
      publish: Boolean,
  ): Set<String> {
    return emptySet()
  }

  override fun rollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean
  ): Set<String> {
    return emptySet()
  }

  override fun dump(): Map<KClass<*>, Map<String, Record>> {
    return emptyMap()
  }
}
