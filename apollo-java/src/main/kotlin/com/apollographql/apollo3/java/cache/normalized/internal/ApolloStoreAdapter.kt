package com.apollographql.apollo3.java.cache.normalized.internal

import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.cache.normalized.api.CacheHeaders
import com.apollographql.apollo3.cache.normalized.api.CacheKey
import com.apollographql.apollo3.cache.normalized.api.CacheKeyGenerator
import com.apollographql.apollo3.cache.normalized.api.CacheResolver
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.api.Record
import com.apollographql.apollo3.java.Subscription
import com.apollographql.apollo3.java.cache.normalized.ApolloStore
import com.apollographql.apollo3.java.internal.launchToSubscription
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.function.Function
import com.apollographql.apollo3.cache.normalized.ApolloStore as ApolloKotlinStore

internal class ApolloStoreAdapter(private val apolloKotlinStore: ApolloKotlinStore) : ApolloStore {
  override fun observeChangedKeys(callback: Function<Set<String>, Void>): Subscription {
    return launchToSubscription {
      apolloKotlinStore.changedKeys.collect {
        callback.apply(it)
      }
    }
  }

  override fun <D : Operation.Data> readOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D {
    return runBlocking { apolloKotlinStore.readOperation(operation, customScalarAdapters, cacheHeaders) }
  }

  override fun <D : Operation.Data> readOperation(operation: Operation<D>): D {
    return runBlocking { apolloKotlinStore.readOperation(operation) }
  }

  override fun <D : Fragment.Data> readFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
  ): D {
    return runBlocking { apolloKotlinStore.readFragment(fragment, cacheKey, customScalarAdapters, cacheHeaders) }
  }

  override fun <D : Fragment.Data> readFragment(fragment: Fragment<D>, cacheKey: CacheKey): D {
    return runBlocking { apolloKotlinStore.readFragment(fragment, cacheKey) }
  }

  override fun <D : Operation.Data> writeOperation(
      operation: Operation<D>,
      operationData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    return runBlocking { apolloKotlinStore.writeOperation(operation, operationData, customScalarAdapters, cacheHeaders, publish) }
  }

  override fun <D : Operation.Data> writeOperation(operation: Operation<D>, operationData: D): Set<String> {
    return runBlocking { apolloKotlinStore.writeOperation(operation, operationData) }
  }

  override fun <D : Fragment.Data> writeFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders,
      publish: Boolean,
  ): Set<String> {
    return runBlocking { apolloKotlinStore.writeFragment(fragment, cacheKey, fragmentData, customScalarAdapters, cacheHeaders, publish) }
  }

  override fun <D : Fragment.Data> writeFragment(fragment: Fragment<D>, cacheKey: CacheKey, fragmentData: D): Set<String> {
    return runBlocking { apolloKotlinStore.writeFragment(fragment, cacheKey, fragmentData) }
  }

  override fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: UUID,
      customScalarAdapters: CustomScalarAdapters,
      publish: Boolean,
  ): Set<String> {
    return runBlocking { apolloKotlinStore.writeOptimisticUpdates(operation, operationData, mutationId, customScalarAdapters, publish) }
  }

  override fun <D : Operation.Data> writeOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: UUID,
  ): Set<String> {
    return runBlocking { apolloKotlinStore.writeOptimisticUpdates(operation, operationData, mutationId) }
  }

  override fun rollbackOptimisticUpdates(mutationId: UUID, publish: Boolean) {
    runBlocking { apolloKotlinStore.rollbackOptimisticUpdates(mutationId, publish) }
  }

  override fun rollbackOptimisticUpdates(mutationId: UUID) {
    runBlocking { apolloKotlinStore.rollbackOptimisticUpdates(mutationId) }
  }

  override fun clearAll(): Boolean {
    return apolloKotlinStore.clearAll()
  }

  override fun remove(cacheKey: CacheKey, cascade: Boolean): Boolean {
    return runBlocking { apolloKotlinStore.remove(cacheKey, cascade) }
  }

  override fun remove(cacheKey: CacheKey): Boolean {
    return runBlocking { apolloKotlinStore.remove(cacheKey) }
  }

  override fun remove(cacheKeys: List<CacheKey>, cascade: Boolean): Int {
    return runBlocking { apolloKotlinStore.remove(cacheKeys, cascade) }
  }

  override fun remove(cacheKeys: List<CacheKey>): Int {
    return runBlocking { apolloKotlinStore.remove(cacheKeys) }
  }

  override fun <D : Operation.Data> normalize(
      operation: Operation<D>,
      data: D,
      customScalarAdapters: CustomScalarAdapters,
  ): Map<String, Record> {
    return apolloKotlinStore.normalize(operation, data, customScalarAdapters)
  }

  override fun publish(keys: Set<String>) {
    runBlocking { apolloKotlinStore.publish(keys) }
  }

  override fun <R : Any?> accessCache(block: Function<NormalizedCache, R>): R {
    return runBlocking { apolloKotlinStore.accessCache { block.apply(it) } }
  }

  override fun dump(): Map<Class<*>, Map<String, Record>> {
    return runBlocking { apolloKotlinStore.dump().mapKeys { it.key.java } }
  }

  override fun dispose() {
    apolloKotlinStore.dispose()
  }

  companion object {
    @JvmStatic
    fun createApolloStore(
        normalizedCacheFactory: NormalizedCacheFactory,
        cacheKeyGenerator: CacheKeyGenerator,
        cacheResolver: CacheResolver,
    ): ApolloStoreAdapter {
      return ApolloStoreAdapter(ApolloKotlinStore(normalizedCacheFactory, cacheKeyGenerator, cacheResolver))
    }

    @JvmStatic
    fun createApolloStore(
        normalizedCacheFactory: NormalizedCacheFactory,
    ): ApolloStoreAdapter {
      return ApolloStoreAdapter(ApolloKotlinStore(normalizedCacheFactory))
    }
  }
}
