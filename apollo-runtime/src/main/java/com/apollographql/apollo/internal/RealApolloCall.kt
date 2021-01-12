package com.apollographql.apollo.internal

import com.apollographql.apollo.ApolloCall
import com.apollographql.apollo.ApolloMutationCall
import com.apollographql.apollo.ApolloQueryCall
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.OperationName
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.api.cache.http.HttpCache
import com.apollographql.apollo.api.cache.http.HttpCachePolicy
import com.apollographql.apollo.api.internal.Action
import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.api.internal.Optional
import com.apollographql.apollo.cache.CacheHeaders
import com.apollographql.apollo.cache.normalized.ApolloStore
import com.apollographql.apollo.exception.ApolloCanceledException
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.exception.ApolloNetworkException
import com.apollographql.apollo.exception.ApolloParseException
import com.apollographql.apollo.fetcher.ApolloResponseFetchers
import com.apollographql.apollo.fetcher.ResponseFetcher
import com.apollographql.apollo.interceptor.ApolloAutoPersistedOperationInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.interceptor.ApolloInterceptorFactory
import com.apollographql.apollo.internal.interceptor.ApolloCacheInterceptor
import com.apollographql.apollo.internal.interceptor.ApolloParseInterceptor
import com.apollographql.apollo.internal.interceptor.ApolloServerInterceptor
import com.apollographql.apollo.internal.interceptor.RealApolloInterceptorChain
import com.apollographql.apollo.request.RequestHeaders
import okhttp3.Call
import okhttp3.HttpUrl
import java.util.ArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicReference

class RealApolloCall<D : Operation.Data> internal constructor(builder: Builder<D>) : ApolloQueryCall<D>, ApolloMutationCall<D> {
  val operation: Operation<D>
  val serverUrl: HttpUrl?
  val httpCallFactory: Call.Factory?
  val httpCache: HttpCache?
  val httpCachePolicy: HttpCachePolicy.Policy?
  val customScalarAdapters: CustomScalarAdapters?
  val apolloStore: ApolloStore?
  val cacheHeaders: CacheHeaders?
  val requestHeaders: RequestHeaders
  val responseFetcher: ResponseFetcher?
  val interceptorChain: ApolloInterceptorChain
  val dispatcher: Executor?
  val logger: ApolloLogger?
  val tracker: ApolloCallTracker?
  val applicationInterceptors: List<ApolloInterceptor>?
  val applicationInterceptorFactories: List<ApolloInterceptorFactory>?
  val autoPersistedOperationsInterceptorFactory: ApolloInterceptorFactory?
  val refetchQueryNames: List<OperationName>
  val refetchQueries: List<Query<*>>
  var queryReFetcher: Optional<QueryReFetcher>? = null
  val enableAutoPersistedQueries: Boolean
  val state = AtomicReference(CallState.IDLE)
  val originalCallback = AtomicReference<ApolloCall.Callback<D>?>()
  val optimisticUpdates: Optional<Operation.Data>
  val useHttpGetMethodForQueries: Boolean
  val useHttpGetMethodForPersistedQueries: Boolean
  val writeToNormalizedCacheAsynchronously: Boolean
  override fun enqueue(responseCallback: ApolloCall.Callback<D>?) {
    try {
      activate(Optional.fromNullable(responseCallback))
    } catch (e: ApolloCanceledException) {
      if (responseCallback != null) {
        responseCallback.onCanceledError(e)
      } else {
        logger!!.e(e, "Operation: %s was canceled", operation().name().name())
      }
      return
    }
    val request = InterceptorRequest.builder(operation)
        .cacheHeaders(cacheHeaders!!)
        .requestHeaders(requestHeaders)
        .fetchFromCache(false)
        .optimisticUpdates(optimisticUpdates)
        .useHttpGetMethodForQueries(useHttpGetMethodForQueries)
        .build()
    interceptorChain.proceedAsync(request, dispatcher!!, interceptorCallbackProxy())
  }

  override fun watcher(): RealApolloQueryWatcher<D> {
    return RealApolloQueryWatcher(clone(), apolloStore!!, logger!!, tracker!!, ApolloResponseFetchers.CACHE_FIRST)
  }

  override fun httpCachePolicy(httpCachePolicy: HttpCachePolicy.Policy): RealApolloCall<D> {
    check(state.get() == CallState.IDLE) { "Already Executed" }
    return toBuilder()
        .httpCachePolicy(httpCachePolicy)
        .build()
  }

  override fun responseFetcher(fetcher: ResponseFetcher): RealApolloCall<D> {
    check(state.get() == CallState.IDLE) { "Already Executed" }
    return toBuilder()
        .responseFetcher(fetcher)
        .build()
  }

  override fun cacheHeaders(cacheHeaders: CacheHeaders): RealApolloCall<D> {
    check(state.get() == CallState.IDLE) { "Already Executed" }
    return toBuilder()
        .cacheHeaders(cacheHeaders)
        .build()
  }

  override fun requestHeaders(requestHeaders: RequestHeaders): RealApolloCall<D> {
    check(state.get() == CallState.IDLE) { "Already Executed" }
    return toBuilder()
        .requestHeaders(requestHeaders)
        .build()
  }

  @Synchronized
  override fun cancel() {
    when (state.get()) {
      CallState.ACTIVE -> {
        state.set(CallState.CANCELED)
        try {
          interceptorChain.dispose()
          if (queryReFetcher!!.isPresent) {
            queryReFetcher!!.get().cancel()
          }
        } finally {
          tracker!!.unregisterCall(this)
          originalCallback.set(null)
        }
      }
      CallState.IDLE -> state.set(CallState.CANCELED)
      CallState.CANCELED, CallState.TERMINATED -> {
      }
      else -> throw IllegalStateException("Unknown state")
    }
  }

  override val isCanceled: Boolean
    get() = state.get() == CallState.CANCELED

  override fun clone(): RealApolloCall<D> {
    return toBuilder().build()
  }

  override fun refetchQueries(vararg operationNames: OperationName): ApolloMutationCall<D> {
    check(state.get() == CallState.IDLE) { "Already Executed" }
    return toBuilder()
        .refetchQueryNames(operationNames.toList())
        .build()
  }

  override fun refetchQueries(vararg queries: Query<*>): ApolloMutationCall<D> {
    check(state.get() == CallState.IDLE) { "Already Executed" }
    return toBuilder()
        .refetchQueries(queries.toList())
        .build()
  }

  override fun operation(): Operation<D> {
    return operation
  }

  private fun interceptorCallbackProxy(): CallBack {
    return object : CallBack {
      override fun onResponse(response: InterceptorResponse) {
        val callback = responseCallback()
        if (!callback.isPresent) {
          logger!!.d("onResponse for operation: %s. No callback present.", operation().name().name())
          return
        }
        callback.get().onResponse(response.parsedResponse.get() as Response<D>)
      }

      override fun onFailure(e: ApolloException) {
        val callback = terminate()
        if (!callback.isPresent) {
          logger!!.d(e, "onFailure for operation: %s. No callback present.", operation().name().name())
          return
        }
        if (e is ApolloHttpException) {
          callback.get().onHttpError(e)
        } else if (e is ApolloParseException) {
          callback.get().onParseError(e)
        } else if (e is ApolloNetworkException) {
          callback.get().onNetworkError(e)
        } else {
          callback.get().onFailure(e)
        }
      }

      override fun onCompleted() {
        val callback = terminate()
        if (queryReFetcher!!.isPresent) {
          queryReFetcher!!.get().refetch()
        }
        if (!callback.isPresent) {
          logger!!.d("onCompleted for operation: %s. No callback present.", operation().name().name())
          return
        }
        callback.get().onStatusEvent(ApolloCall.StatusEvent.COMPLETED)
      }

      override fun onFetch(sourceType: FetchSourceType) {
        responseCallback().apply(object : Action<ApolloCall.Callback<D>> {
          override fun apply(t: ApolloCall.Callback<D>) {
            when (sourceType) {
              FetchSourceType.CACHE -> t.onStatusEvent(ApolloCall.StatusEvent.FETCH_CACHE)
              FetchSourceType.NETWORK -> t.onStatusEvent(ApolloCall.StatusEvent.FETCH_NETWORK)
            }
          }
        })
      }
    }
  }

  override fun toBuilder(): Builder<D> {
    return builder<D>()
        .operation(operation)
        .serverUrl(serverUrl)
        .httpCallFactory(httpCallFactory)
        .httpCache(httpCache)
        .httpCachePolicy(httpCachePolicy!!)
        .scalarTypeAdapters(customScalarAdapters)
        .apolloStore(apolloStore)
        .cacheHeaders(cacheHeaders!!)
        .requestHeaders(requestHeaders)
        .responseFetcher(responseFetcher!!)
        .dispatcher(dispatcher)
        .logger(logger)
        .applicationInterceptors(applicationInterceptors)
        .applicationInterceptorFactories(applicationInterceptorFactories)
        .autoPersistedOperationsInterceptorFactory(autoPersistedOperationsInterceptorFactory)
        .tracker(tracker)
        .refetchQueryNames(refetchQueryNames)
        .refetchQueries(refetchQueries)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .useHttpGetMethodForQueries(useHttpGetMethodForQueries)
        .useHttpGetMethodForPersistedQueries(useHttpGetMethodForPersistedQueries)
        .optimisticUpdates(optimisticUpdates)
        .writeToNormalizedCacheAsynchronously(writeToNormalizedCacheAsynchronously)
  }

  @Synchronized
  private fun activate(callback: Optional<ApolloCall.Callback<D>>) {
    when (state.get()) {
      CallState.IDLE -> {
        originalCallback.set(callback.orNull())
        tracker!!.registerCall(this)
        callback.apply(object : Action<ApolloCall.Callback<D>> {
          override fun apply(t: ApolloCall.Callback<D>) {
            t.onStatusEvent(ApolloCall.StatusEvent.SCHEDULED)
          }
        })
      }
      CallState.CANCELED -> throw ApolloCanceledException()
      CallState.TERMINATED, CallState.ACTIVE -> throw IllegalStateException("Already Executed")
      else -> throw IllegalStateException("Unknown state")
    }
    state.set(CallState.ACTIVE)
  }

  @Synchronized
  fun responseCallback(): Optional<ApolloCall.Callback<D>> {
    return when (state.get()) {
      CallState.ACTIVE, CallState.CANCELED -> Optional.fromNullable(originalCallback.get())
      CallState.IDLE, CallState.TERMINATED -> throw IllegalStateException(
          CallState.IllegalStateMessage.forCurrentState(state.get()).expected(CallState.ACTIVE, CallState.CANCELED))
      else -> throw IllegalStateException("Unknown state")
    }
  }

  @Synchronized
  fun terminate(): Optional<ApolloCall.Callback<D>> {
    return when (state.get()) {
      CallState.ACTIVE -> {
        tracker!!.unregisterCall(this)
        state.set(CallState.TERMINATED)
        Optional.fromNullable(originalCallback.getAndSet(null))
      }
      CallState.CANCELED -> Optional.fromNullable(originalCallback.getAndSet(null))
      CallState.IDLE, CallState.TERMINATED -> throw IllegalStateException(
          CallState.IllegalStateMessage.forCurrentState(state.get()).expected(CallState.ACTIVE, CallState.CANCELED))
      else -> throw IllegalStateException("Unknown state")
    }
  }

  private fun prepareInterceptorChain(operation: Operation<*>): ApolloInterceptorChain {
    val httpCachePolicy = if (operation is Query<*>) httpCachePolicy else null
    val interceptors: MutableList<ApolloInterceptor> = ArrayList()
    for (factory in applicationInterceptorFactories!!) {
      val interceptor = factory.newInterceptor(logger!!, operation)
      if (interceptor != null) {
        interceptors.add(interceptor)
      }
    }
    interceptors.addAll(applicationInterceptors!!)
    interceptors.add(responseFetcher!!.provideInterceptor(logger))
    interceptors.add(ApolloCacheInterceptor(
        apolloStore!!,
        apolloStore.networkResponseNormalizer(),
        customScalarAdapters!!,
        dispatcher!!,
        logger!!,
        writeToNormalizedCacheAsynchronously))
    if (autoPersistedOperationsInterceptorFactory != null) {
      val interceptor = autoPersistedOperationsInterceptorFactory.newInterceptor(logger, operation)
      if (interceptor != null) {
        interceptors.add(interceptor)
      }
    } else {
      if (enableAutoPersistedQueries && (operation is Query<*> || operation is Mutation<*>)) {
        interceptors.add(ApolloAutoPersistedOperationInterceptor(
            logger,
            useHttpGetMethodForPersistedQueries && operation !is Mutation<*>))
      }
    }
    interceptors.add(ApolloParseInterceptor(
        httpCache,
        customScalarAdapters,
        logger))
    interceptors.add(ApolloServerInterceptor(serverUrl!!, httpCallFactory!!, httpCachePolicy, false, customScalarAdapters,
        logger))
    return RealApolloInterceptorChain(interceptors)
  }

  class Builder<D : Operation.Data> internal constructor() : ApolloQueryCall.Builder<D>, ApolloMutationCall.Builder<D> {
    var operation: Operation<*>? = null
    var serverUrl: HttpUrl? = null
    var httpCallFactory: Call.Factory? = null
    var httpCache: HttpCache? = null
    var httpCachePolicy: HttpCachePolicy.Policy? = null
    var customScalarAdapters: CustomScalarAdapters? = null
    var apolloStore: ApolloStore? = null
    var responseFetcher: ResponseFetcher? = null
    var cacheHeaders: CacheHeaders? = null
    var requestHeaders = RequestHeaders.NONE
    var dispatcher: Executor? = null
    var logger: ApolloLogger? = null
    var applicationInterceptors: List<ApolloInterceptor>? = null
    var applicationInterceptorFactories: List<ApolloInterceptorFactory>? = null
    var autoPersistedOperationsInterceptorFactory: ApolloInterceptorFactory? = null
    var refetchQueryNames: List<OperationName> = emptyList()
    var refetchQueries: List<Query<*>> = emptyList()
    var tracker: ApolloCallTracker? = null
    var enableAutoPersistedQueries = false
    var optimisticUpdates = Optional.absent<Operation.Data>()
    var useHttpGetMethodForQueries = false
    var useHttpGetMethodForPersistedQueries = false
    var writeToNormalizedCacheAsynchronously = false
    fun operation(operation: Operation<*>?): Builder<D> {
      this.operation = operation
      return this
    }

    fun serverUrl(serverUrl: HttpUrl?): Builder<D> {
      this.serverUrl = serverUrl
      return this
    }

    fun httpCallFactory(httpCallFactory: Call.Factory?): Builder<D> {
      this.httpCallFactory = httpCallFactory
      return this
    }

    fun httpCache(httpCache: HttpCache?): Builder<D> {
      this.httpCache = httpCache
      return this
    }

    fun scalarTypeAdapters(customScalarAdapters: CustomScalarAdapters?): Builder<D> {
      this.customScalarAdapters = customScalarAdapters
      return this
    }

    fun apolloStore(apolloStore: ApolloStore?): Builder<D> {
      this.apolloStore = apolloStore
      return this
    }

    override fun cacheHeaders(cacheHeaders: CacheHeaders): Builder<D> {
      this.cacheHeaders = cacheHeaders
      return this
    }

    override fun httpCachePolicy(httpCachePolicy: HttpCachePolicy.Policy): Builder<D> {
      this.httpCachePolicy = httpCachePolicy
      return this
    }

    override fun responseFetcher(responseFetcher: ResponseFetcher): Builder<D> {
      this.responseFetcher = responseFetcher
      return this
    }

    override fun requestHeaders(requestHeaders: RequestHeaders): Builder<D> {
      this.requestHeaders = requestHeaders
      return this
    }

    override fun refetchQueryNames(refetchQueryNames: List<OperationName>): Builder<D> {
      this.refetchQueryNames = ArrayList(refetchQueryNames)
      return this
    }

    override fun refetchQueries(refetchQueries: List<Query<*>>): Builder<D> {
      this.refetchQueries = ArrayList(refetchQueries)
      return this
    }

    fun dispatcher(dispatcher: Executor?): Builder<D> {
      this.dispatcher = dispatcher
      return this
    }

    fun logger(logger: ApolloLogger?): Builder<D> {
      this.logger = logger
      return this
    }

    fun tracker(tracker: ApolloCallTracker?): Builder<D> {
      this.tracker = tracker
      return this
    }

    fun applicationInterceptors(applicationInterceptors: List<ApolloInterceptor>?): Builder<D> {
      this.applicationInterceptors = applicationInterceptors
      return this
    }

    fun applicationInterceptorFactories(applicationInterceptorFactories: List<ApolloInterceptorFactory>?): Builder<D> {
      this.applicationInterceptorFactories = applicationInterceptorFactories
      return this
    }

    fun autoPersistedOperationsInterceptorFactory(interceptorFactory: ApolloInterceptorFactory?): Builder<D> {
      autoPersistedOperationsInterceptorFactory = interceptorFactory
      return this
    }

    fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean): Builder<D> {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
      return this
    }

    fun optimisticUpdates(optimisticUpdates: Optional<Operation.Data>): Builder<D> {
      this.optimisticUpdates = optimisticUpdates
      return this
    }

    fun useHttpGetMethodForQueries(useHttpGetMethodForQueries: Boolean): Builder<D> {
      this.useHttpGetMethodForQueries = useHttpGetMethodForQueries
      return this
    }

    fun useHttpGetMethodForPersistedQueries(useHttpGetMethodForPersistedQueries: Boolean): Builder<D> {
      this.useHttpGetMethodForPersistedQueries = useHttpGetMethodForPersistedQueries
      return this
    }

    fun writeToNormalizedCacheAsynchronously(writeToNormalizedCacheAsynchronously: Boolean): Builder<D> {
      this.writeToNormalizedCacheAsynchronously = writeToNormalizedCacheAsynchronously
      return this
    }

    override fun build(): RealApolloCall<D> {
      return RealApolloCall(this)
    }
  }

  companion object {
    @JvmStatic
    fun <D : Operation.Data> builder(): Builder<D> {
      return Builder()
    }
  }

  init {
    operation = builder.operation as Operation<D>
    serverUrl = builder.serverUrl
    httpCallFactory = builder.httpCallFactory
    httpCache = builder.httpCache
    httpCachePolicy = builder.httpCachePolicy
    customScalarAdapters = builder.customScalarAdapters
    apolloStore = builder.apolloStore
    responseFetcher = builder.responseFetcher
    cacheHeaders = builder.cacheHeaders
    requestHeaders = builder.requestHeaders
    dispatcher = builder.dispatcher
    logger = builder.logger
    applicationInterceptors = builder.applicationInterceptors
    applicationInterceptorFactories = builder.applicationInterceptorFactories
    autoPersistedOperationsInterceptorFactory = builder.autoPersistedOperationsInterceptorFactory
    refetchQueryNames = builder.refetchQueryNames
    refetchQueries = builder.refetchQueries
    tracker = builder.tracker
    queryReFetcher = if (refetchQueries.isEmpty() && refetchQueryNames.isEmpty() || builder.apolloStore == null) {
      Optional.absent()
    } else {
      Optional.of(QueryReFetcher.builder()
          .queries(builder.refetchQueries)
          .queryWatchers(refetchQueryNames)
          .serverUrl(builder.serverUrl)
          .httpCallFactory(builder.httpCallFactory)
          .scalarTypeAdapters(builder.customScalarAdapters)
          .apolloStore(builder.apolloStore)
          .dispatcher(builder.dispatcher)
          .logger(builder.logger)
          .applicationInterceptors(builder.applicationInterceptors)
          .applicationInterceptorFactories(builder.applicationInterceptorFactories)
          .autoPersistedOperationsInterceptorFactory(builder.autoPersistedOperationsInterceptorFactory)
          .callTracker(builder.tracker)
          .build())
    }
    useHttpGetMethodForQueries = builder.useHttpGetMethodForQueries
    enableAutoPersistedQueries = builder.enableAutoPersistedQueries
    useHttpGetMethodForPersistedQueries = builder.useHttpGetMethodForPersistedQueries
    optimisticUpdates = builder.optimisticUpdates
    writeToNormalizedCacheAsynchronously = builder.writeToNormalizedCacheAsynchronously
    interceptorChain = prepareInterceptorChain(operation)
  }
}