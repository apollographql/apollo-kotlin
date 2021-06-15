package com.apollographql.apollo3.rx2


import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.exception.CacheMissException
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.Record
import com.benasher44.uuid.Uuid
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.asCoroutineDispatcher
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.rx2.rxSingle
import kotlin.reflect.KClass

class Rx2ApolloStore(
    private val delegate: ApolloStore,
    private val scheduler: Scheduler,
) : ApolloStore by delegate {
  constructor(
      normalizedCacheFactory: NormalizedCacheFactory,
      cacheResolver: CacheResolver,
      scheduler: Scheduler = Schedulers.io(),
  ) : this(ApolloStore(normalizedCacheFactory, cacheResolver), scheduler)

  private val dispatcher = scheduler.asCoroutineDispatcher()

  fun <D : Operation.Data> rxReadOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): Single<D> = rxSingle(dispatcher) {
    readOperation(operation, customScalarAdapters, cacheHeaders) ?: throw CacheMissException("Cache miss excpetion for $operation")
  }

  fun <D : Fragment.Data> rxReadFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): Single<D> = rxSingle(dispatcher) {
    readFragment(fragment, cacheKey, customScalarAdapters, cacheHeaders) ?: throw CacheMissException("Cache miss excpetion for $fragment")
  }

  fun <D : Operation.Data> rxWriteOperation(
      operation: Operation<D>,
      operationData: D,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
      publish: Boolean = true,
  ): Single<Set<String>> = rxSingle(dispatcher) {
    writeOperation(operation, operationData, customScalarAdapters, cacheHeaders, publish)
  }

  fun <D : Fragment.Data> rxWriteFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
      publish: Boolean = true,
  ): Single<Set<String>> = rxSingle(dispatcher) {
    writeFragment(fragment, cacheKey, fragmentData, customScalarAdapters, cacheHeaders, publish)
  }

  fun <D : Operation.Data> rxWriteOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      publish: Boolean = true,
  ): Single<Set<String>> = rxSingle(dispatcher) {
    writeOptimisticUpdates(operation, operationData, mutationId, customScalarAdapters, publish)
  }

  fun rxRollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean = true,
  ): Single<Set<String>> = rxSingle(dispatcher) {
    rollbackOptimisticUpdates(mutationId, publish)
  }

  fun rxRemove(cacheKey: CacheKey, cascade: Boolean = true) = rxSingle(dispatcher) {
    remove(cacheKey, cascade)
  }

  fun rxRemove(cacheKeys: List<CacheKey>, cascade: Boolean = true) = rxSingle(dispatcher) {
    remove(cacheKeys, cascade)
  }

  fun rxPublish(keys: Set<String>) = rxCompletable(dispatcher) {
    publish(keys)
  }

  fun <R : Any> rxAccessCache(block: (NormalizedCache) -> R) = rxSingle(dispatcher) {
    accessCache(block)
  }

  fun rxDump() = rxSingle(dispatcher) {
    dump()
  }
}
