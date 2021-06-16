package com.apollographql.apollo3.rx2

import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.Fragment
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.exception.CacheMissException
import com.apollographql.apollo3.api.http.HttpRequest
import com.apollographql.apollo3.api.http.HttpResponse
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKey
import com.apollographql.apollo3.cache.normalized.CacheResolver
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpInterceptorChain
import com.benasher44.uuid.Uuid
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.rx2.asCoroutineDispatcher
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.rx2.rxFlowable
import kotlinx.coroutines.rx2.rxSingle

fun NetworkTransport.toRx2NetworkTransport(scheduler: Scheduler = Schedulers.io()): Rx2NetworkTransport = object : Rx2NetworkTransport {
  override fun <D : Operation.Data> execute(request: ApolloRequest<D>): Flowable<ApolloResponse<D>> {
    return this@toRx2NetworkTransport.execute(request).asFlowable(scheduler.asCoroutineDispatcher())
  }
  override fun dispose() {
    this@toRx2NetworkTransport.dispose()
  }
}

fun HttpInterceptorChain.toRx2HttpInterceptorChain(scheduler: Scheduler = Schedulers.io()): Rx2HttpInterceptorChain = object : Rx2HttpInterceptorChain {
  override fun proceed(request: HttpRequest): Single<HttpResponse> = rxSingle( scheduler.asCoroutineDispatcher()){
    this@toRx2HttpInterceptorChain.proceed(request)
  }
}

fun HttpInterceptor.toRx2HttpInterceptor(scheduler: Scheduler = Schedulers.io()): Rx2HttpInterceptor = object : Rx2HttpInterceptor {
  override fun intercept(request: HttpRequest, chain: Rx2HttpInterceptorChain): Single<HttpResponse> = rxSingle(scheduler.asCoroutineDispatcher()) {
    this@toRx2HttpInterceptor.intercept(request, chain.toHttpInterceptorChain())
  }
}

fun ApolloInterceptorChain.toRx2ApolloInterceptorChain(scheduler: Scheduler = Schedulers.io()): Rx2ApolloInterceptorChain = object : Rx2ApolloInterceptorChain {
  override fun <D : Operation.Data> proceed(request: ApolloRequest<D>): Flowable<ApolloResponse<D>> {
    return this@toRx2ApolloInterceptorChain.proceed(request).asFlowable(scheduler.asCoroutineDispatcher())
  }
}

fun ApolloInterceptor.toRx2ApolloInterceptor(scheduler: Scheduler = Schedulers.io()): Rx2ApolloInterceptor = object : Rx2ApolloInterceptor {
  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: Rx2ApolloInterceptorChain): Flowable<ApolloResponse<D>> {
    return this@toRx2ApolloInterceptor.intercept(request, chain.toApolloInterceptorChain()).asFlowable(scheduler.asCoroutineDispatcher())
  }
}

fun ApolloStore.toRx2ApolloStore(scheduler: Scheduler = Schedulers.io()) = Rx2ApolloStore(this, scheduler)

class Rx2ApolloStore(
    private val delegate: ApolloStore,
    private val scheduler: Scheduler,
) {
  private val dispatcher = scheduler.asCoroutineDispatcher()

  fun <D : Operation.Data> rxReadOperation(
      operation: Operation<D>,
      customScalarAdapters: CustomScalarAdapters,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): Single<D> = rxSingle(dispatcher) {
    delegate.readOperation(operation, customScalarAdapters, cacheHeaders) ?: throw CacheMissException("Cache miss excpetion for $operation")
  }

  fun <D : Fragment.Data> rxReadFragment(
      fragment: Fragment<D>,
      cacheKey: CacheKey,
      customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
      cacheHeaders: CacheHeaders = CacheHeaders.NONE,
  ): Single<D> = rxSingle(dispatcher) {
    delegate.readFragment(fragment, cacheKey, customScalarAdapters, cacheHeaders) ?: throw CacheMissException("Cache miss excpetion for $fragment")
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

fun ApolloClient.toRx2ApolloClient(scheduler: Scheduler = Schedulers.io()) = Rx2ApolloClient(this, scheduler)

class Rx2ApolloClient(private val delegate: ApolloClient, private val scheduler: Scheduler) {
  private val dispatcher = scheduler.asCoroutineDispatcher()

  fun <D : Query.Data> query(query: Query<D>): Single<ApolloResponse<D>> = query(ApolloRequest(query))

  fun <D : Mutation.Data> mutate(mutation: Mutation<D>): Single<ApolloResponse<D>> = mutate(ApolloRequest(mutation))

  fun <D : Subscription.Data> subscribe(subscription: Subscription<D>): Flowable<ApolloResponse<D>> = subscribe(ApolloRequest(subscription))

  fun <D : Query.Data> query(queryRequest: ApolloRequest<D>): Single<ApolloResponse<D>> = rxSingle(dispatcher) {
    delegate.query(queryRequest)
  }

  fun <D : Query.Data> queryAsFlow(queryRequest: ApolloRequest<D>): Flowable<ApolloResponse<D>>  {
    return delegate.queryAsFlow(queryRequest).asFlowable(dispatcher)
  }

  fun <D : Mutation.Data> mutate(mutationRequest: ApolloRequest<D>): Single<ApolloResponse<D>> = rxSingle(dispatcher) {
    delegate.mutate(mutationRequest)
  }

  fun <D : Mutation.Data> mutateAsFlow(mutationRequest: ApolloRequest<D>): Flowable<ApolloResponse<D>> {
    return delegate.mutateAsFlow(mutationRequest).asFlowable(dispatcher)
  }

  fun <D : Operation.Data> subscribe(subscriptionRequest: ApolloRequest<D>): Flowable<ApolloResponse<D>> {
    return delegate.subscribe(subscriptionRequest).asFlowable(dispatcher)
  }

  fun dispose() {
    delegate.dispose()
  }
}
