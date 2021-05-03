package com.apollographql.apollo3.cache.normalized.internal

import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.Record
import com.benasher44.uuid.Uuid
import kotlinx.coroutines.flow.SharedFlow
import kotlin.reflect.KClass

/**
 * An alternative to RealApolloStore for when a no-operation cache is needed.
 */
internal class NoOpApolloStore : ApolloStore() {
  override val changedKeys: SharedFlow<Set<String>>
    get() = throw NotImplementedError()

  override fun subscribe(subscriber: RecordChangeSubscriber) {}
  override fun unsubscribe(subscriber: RecordChangeSubscriber) {}
  override suspend fun publish(keys: Set<String>) {}
  override fun clearAll(): Boolean {
    return false
  }

  override suspend fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return false
  }

  override suspend fun remove(cacheKeys: List<CacheKey>, cascade: Boolean): Int {
    return 0
  }

  override fun <D : Operation.Data> normalize(operation: Operation<D>, data: D, customScalarAdapters: CustomScalarAdapters): Map<String, Record> {
    return emptyMap()
  }

  override suspend fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      mode: ReadMode,
  ): D? {
    // This will be seen as a cache MISS and the request will go to the network.
    return null
  }

  override suspend fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D? {
    return null
  }

  override suspend fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    return emptySet()
  }

  override suspend fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    return emptySet()
  }


  override suspend fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      customScalarAdapters: CustomScalarAdapters,
      publish: Boolean,
  ): Set<String> {
    return emptySet()
  }

  override suspend fun rollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean
  ): Set<String> {
    return emptySet()
  }

  override suspend fun dump(): Map<KClass<*>, Map<String, Record>> {
    return emptyMap()
  }
}
