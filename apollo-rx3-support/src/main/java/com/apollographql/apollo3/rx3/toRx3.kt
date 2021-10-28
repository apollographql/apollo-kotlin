/*
 * This file is auto generated from apollo-rx2-support by rxjava3.main.kts, do not edit manually.
 */
package com.apollographql.apollo3.rx3

import com.apollographql.apollo3.ApolloMutationCall
import com.apollographql.apollo3.ApolloQueryCall
import com.apollographql.apollo3.ApolloSubscriptionCall
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.benasher44.uuid.Uuid
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.rx3.asCoroutineDispatcher
import kotlinx.coroutines.rx3.asFlowable
import kotlinx.coroutines.rx3.rxCompletable
import kotlinx.coroutines.rx3.rxSingle

fun NetworkTransport.toRx3NetworkTransport(scheduler: Scheduler = Schedulers.io()): Rx3NetworkTransport = object : Rx3NetworkTransport {
  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flowable<ApolloResponse<D>> {
    return this@toRx3NetworkTransport.execute(request).asFlowable(scheduler.asCoroutineDispatcher())
  }
  override fun dispose() {
    this@toRx3NetworkTransport.dispose()
  }
}

fun HttpInterceptorChain.toRx3HttpInterceptorChain(scheduler: Scheduler = Schedulers.io()): Rx3HttpInterceptorChain = object : Rx3HttpInterceptorChain {
  override fun proceed(request: HttpRequest): Single<HttpResponse> = rxSingle( scheduler.asCoroutineDispatcher()){
    this@toRx3HttpInterceptorChain.proceed(request)
  }
}

fun HttpInterceptor.toRx3HttpInterceptor(scheduler: Scheduler = Schedulers.io()): Rx3HttpInterceptor = object : Rx3HttpInterceptor {
  override fun intercept(request: HttpRequest, chain: Rx3HttpInterceptorChain): Single<HttpResponse> = rxSingle(scheduler.asCoroutineDispatcher()) {
    this@toRx3HttpInterceptor.intercept(request, chain.toHttpInterceptorChain())
  }
}

fun ApolloInterceptorChain.toRx3ApolloInterceptorChain(scheduler: Scheduler = Schedulers.io()): Rx3ApolloInterceptorChain = object : Rx3ApolloInterceptorChain {
  override fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flowable<ApolloResponse<D>> {
    return this@toRx3ApolloInterceptorChain.proceed(request).asFlowable(scheduler.asCoroutineDispatcher())
  }
}

fun ApolloInterceptor.toRx3ApolloInterceptor(scheduler: Scheduler = Schedulers.io()): Rx3ApolloInterceptor = object : Rx3ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: Rx3ApolloInterceptorChain): Flowable<ApolloResponse<D>> {
    return this@toRx3ApolloInterceptor.intercept(request, chain.toApolloInterceptorChain()).asFlowable(scheduler.asCoroutineDispatcher())
  }
}

fun ApolloStore.toRx3ApolloStore(scheduler: Scheduler = Schedulers.io()) = Rx3ApolloStore(this, scheduler)

class Rx3ApolloStore(
    private val delegate: ApolloStore,
    private val scheduler: Scheduler,
) {
  private val dispatcher = scheduler.asCoroutineDispatcher()

  fun <D : Operation.Data> rxReadOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): Single<D> = rxSingle(dispatcher) {
    delegate.readOperation(operation, customScalarAdapters, cacheHeaders)
  }

  fun <D : Fragment.Data> rxReadFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): Single<D> = rxSingle(dispatcher) {
    delegate.readFragment(fragment, cacheKey, customScalarAdapters, cacheHeaders)
  }

  fun <D : Operation.Data> rxWriteOperation(
      operation: Operation<D>,
      operationData: D,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
      publish: Boolean = true,
  ): Single<Set<String>> = rxSingle(dispatcher) {
    delegate.writeOperation(operation, operationData, customScalarAdapters, cacheHeaders, publish)
  }

  fun <D : Fragment.Data> rxWriteFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      fragmentData: D,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
      publish: Boolean = true,
  ): Single<Set<String>> = rxSingle(dispatcher) {
    delegate.writeFragment(fragment, cacheKey, fragmentData, customScalarAdapters, cacheHeaders, publish)
  }

  fun <D : Operation.Data> rxWriteOptimisticUpdates(
      operation: Operation<D>,
      operationData: D,
      mutationId: Uuid,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      publish: Boolean = true,
  ): Single<Set<String>> = rxSingle(dispatcher) {
    delegate.writeOptimisticUpdates(operation, operationData, mutationId, customScalarAdapters, publish)
  }

  fun rxRollbackOptimisticUpdates(
      mutationId: Uuid,
      publish: Boolean = true,
  ): Single<Set<String>> = rxSingle(dispatcher) {
    delegate.rollbackOptimisticUpdates(mutationId, publish)
  }

  fun rxRemove(cacheKey: CacheKey, cascade: Boolean = true) = rxSingle(dispatcher) {
    delegate.remove(cacheKey, cascade)
  }

  fun rxRemove(cacheKeys: List<CacheKey>, cascade: Boolean = true) = rxSingle(dispatcher) {
    delegate.remove(cacheKeys, cascade)
  }

  fun rxPublish(keys: Set<String>) = rxCompletable(dispatcher) {
    delegate.publish(keys)
  }

  fun <R : Any> rxAccessCache(block: (NormalizedCache) -> R) = rxSingle(dispatcher) {
    delegate.accessCache(block)
  }

  fun rxDump() = rxSingle(dispatcher) {
    delegate.dump()
  }
}

fun <D: Query.Data> ApolloQueryCall<D>.rxSingle(scheduler: Scheduler) = rxSingle(scheduler.asCoroutineDispatcher()) {
  execute()
}
fun <D: Mutation.Data> ApolloMutationCall<D>.rxSingle(scheduler: Scheduler) = rxSingle(scheduler.asCoroutineDispatcher()) {
  execute()
}
fun <D: Subscription.Data> ApolloSubscriptionCall<D>.rxFlowable(scheduler: Scheduler) = execute().asFlowable(scheduler.asCoroutineDispatcher())
