package com.apollographql.apollo3

import com.apollographql.apollo3.ApolloClient.Builder
import com.apollographql.apollo3.api.CustomScalar
import com.apollographql.apollo3.api.CustomScalarAdapter
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.ResponseAdapterCache
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.cache.http.HttpCache
import com.apollographql.apollo3.api.cache.http.HttpCachePolicy
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.Optional
import com.apollographql.apollo3.api.internal.Optional.Companion.absent
import com.apollographql.apollo3.api.internal.Optional.Companion.fromNullable
import com.apollographql.apollo3.api.internal.Optional.Companion.of
import com.apollographql.apollo3.cache.CacheHeaders
import com.apollographql.apollo3.cache.normalized.ApolloStore
import com.apollographql.apollo3.cache.normalized.CacheKeyResolver
import com.apollographql.apollo3.cache.normalized.NormalizedCache
import com.apollographql.apollo3.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.internal.RealApolloStore
import com.apollographql.apollo3.fetcher.ApolloResponseFetchers
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptorFactory
import com.apollographql.apollo3.internal.ApolloCallTracker
import com.apollographql.apollo3.internal.RealApolloCall
import com.apollographql.apollo3.internal.RealApolloPrefetch
import com.apollographql.apollo3.internal.RealApolloSubscriptionCall
import com.apollographql.apollo3.internal.subscription.NoOpSubscriptionManager
import com.apollographql.apollo3.internal.subscription.RealSubscriptionManager
import com.apollographql.apollo3.internal.subscription.SubscriptionManager
import com.apollographql.apollo3.subscription.OnSubscriptionManagerStateChangeListener
import com.apollographql.apollo3.subscription.SubscriptionConnectionParams
import com.apollographql.apollo3.subscription.SubscriptionConnectionParamsProvider
import com.apollographql.apollo3.subscription.SubscriptionTransport
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.ArrayList
import java.util.Collections
import java.util.LinkedHashMap
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/**
 * ApolloClient class represents the abstraction for the graphQL client that will be used to execute queries and read the responses back.
 *
 * <h3>ApolloClient should be shared</h3>
 *
 *
 * Since each ApolloClient holds its own connection pool and thread pool, it is recommended to only create a single ApolloClient and use
 * that for execution of all the queries, as this would reduce latency and would also save memory. Conversely, creating a client for each
 * query execution would result in resource wastage on idle pools.
 *
 *
 *
 * See the [ApolloClient.Builder] class for configuring the ApolloClient.
 */
class ApolloClient internal constructor(
    /**
     * @return The [HttpUrl] serverUrl
     */
    val serverUrl: HttpUrl?,
    private val httpCallFactory: Call.Factory?,
    /**
     * @return The [HttpCache] httpCache
     */
    val httpCache: HttpCache?,
    /**
     * @return The [ApolloStore] managing access to the normalized cache created by
     * [Builder.normalizedCache]  }
     */
    val apolloStore: ApolloStore,
    /**
     * @return The [ResponseAdapterCache] scalarTypeAdapters
     */
    val scalarTypeAdapters: ResponseAdapterCache,
    private val dispatcher: Executor?,
    private val defaultHttpCachePolicy: HttpCachePolicy.Policy,
    private val defaultResponseFetcher: ResponseFetcher,
    /**
     * @return The default [CacheHeaders] which this instance of [ApolloClient] was configured.
     */
    val defaultCacheHeaders: CacheHeaders,
    private val logger: ApolloLogger,
    applicationInterceptors: List<ApolloInterceptor>,
    applicationInterceptorFactories: List<ApolloInterceptorFactory>,
    autoPersistedOperationsInterceptorFactory: ApolloInterceptorFactory?,
    enableAutoPersistedQueries: Boolean,
    subscriptionManager: SubscriptionManager,
    useHttpGetMethodForQueries: Boolean,
    useHttpGetMethodForPersistedQueries: Boolean,
    writeToNormalizedCacheAsynchronously: Boolean) : ApolloQueryCall.Factory, ApolloMutationCall.Factory, ApolloPrefetch.Factory, ApolloSubscriptionCall.Factory {
  private val tracker = ApolloCallTracker()
  private val applicationInterceptors: List<ApolloInterceptor>
  private val applicationInterceptorFactories: List<ApolloInterceptorFactory>

  /**
   * @return The [ApolloInterceptor] used for auto persisted operations
   */
  val autoPersistedOperationsInterceptorFactory: ApolloInterceptorFactory?
  private val enableAutoPersistedQueries: Boolean
  val subscriptionManager: SubscriptionManager
  private val useHttpGetMethodForQueries: Boolean
  private val useHttpGetMethodForPersistedQueries: Boolean
  private val writeToNormalizedCacheAsynchronously: Boolean
  override fun <D : Operation.Data> mutate(
      mutation: Mutation<D>): ApolloMutationCall<D> {
    return newCall(mutation).responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
  }

  override fun <D : Operation.Data> mutate(
      mutation: Mutation<D>, withOptimisticUpdates: D): ApolloMutationCall<D> {
    return newCall(mutation).toBuilder().responseFetcher(ApolloResponseFetchers.NETWORK_ONLY)
        .optimisticUpdates(fromNullable(withOptimisticUpdates)).build()
  }

  override fun <D : Operation.Data> query(query: Query<D>): ApolloQueryCall<D> {
    return newCall(query)
  }

  override fun <D : Operation.Data> prefetch(
      operation: Operation<D>): ApolloPrefetch {
    return RealApolloPrefetch(operation, serverUrl!!, httpCallFactory!!, scalarTypeAdapters, dispatcher!!, logger,
        tracker)
  }

  override fun <D : Operation.Data> subscribe(
      subscription: Subscription<D>): ApolloSubscriptionCall<D> {
    return RealApolloSubscriptionCall(subscription, subscriptionManager, apolloStore, ApolloSubscriptionCall.CachePolicy.NO_CACHE,
        dispatcher!!, logger)
  }

  /**
   * Adds new listener for subscription manager state changes.
   *
   * @param onStateChangeListener to be called when state changed
   */
  fun addOnSubscriptionManagerStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    subscriptionManager.addOnStateChangeListener(onStateChangeListener)
  }

  /**
   * Removes listener for subscription manager state changes.
   *
   * @param onStateChangeListener to remove
   */
  fun removeOnSubscriptionManagerStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    subscriptionManager.removeOnStateChangeListener(onStateChangeListener)
  }

  /**
   * Call [start][SubscriptionManager.start] on the subscriptionManager. Which will put the subscriptionManager in a connectible state
   * if its current state is STOPPED. This is a noop if the current state is anything other than STOPPED.
   *
   *
   * When subscriptions are re-enabled after having been disabled, the underlying transport isn't reconnected immediately, but will be on
   * the first new subscription created.
   */
  fun enableSubscriptions() {
    subscriptionManager.start()
  }

  /**
   * Call [stop][SubscriptionManager.stop] on the subscriptionManager. Which will unsubscribe from all active subscriptions, disconnect
   * the underlying transport (eg websocket), and put the subscriptionManager in the STOPPED state.
   *
   *
   * New subscriptions will fail until [.enableSubscriptions] is called.
   */
  fun disableSubscriptions() {
    subscriptionManager.stop()
  }

  /**
   * Clear all entries from the [HttpCache], if present.
   */
  fun clearHttpCache() {
    httpCache?.clear()
  }

  /**
   * @return The list of [ApolloInterceptor]s
   */
  fun getApplicationInterceptors(): List<ApolloInterceptor> {
    return Collections.unmodifiableList(applicationInterceptors)
  }

  /**
   * @return The list of [ApolloInterceptorFactory]
   */
  fun getApplicationInterceptorFactories(): List<ApolloInterceptorFactory> {
    return Collections.unmodifiableList(applicationInterceptorFactories)
  }

  /**
   * Sets the idleResourceCallback which will be called when this ApolloClient is idle.
   */
  fun idleCallback(idleResourceCallback: IdleResourceCallback?) {
    tracker.setIdleResourceCallback(idleResourceCallback)
  }

  /**
   * Returns the count of [ApolloCall] & [ApolloPrefetch] objects which are currently in progress.
   */
  fun activeCallsCount(): Int {
    return tracker.activeCallsCount()
  }

  /**
   * @return a new instance of [Builder] to customize an existing [ApolloClient]
   */
  fun newBuilder(): Builder {
    return Builder(this)
  }

  @Throws(IOException::class)
  fun cachedHttpResponse(cacheKey: String?): Response? {
    return httpCache?.read(cacheKey!!)
  }

  private fun <D : Operation.Data> newCall(
      operation: Operation<D>): RealApolloCall<D> {
    return RealApolloCall.builder<D>()
        .operation(operation)
        .serverUrl(serverUrl)
        .httpCallFactory(httpCallFactory)
        .httpCache(httpCache)
        .httpCachePolicy(defaultHttpCachePolicy)
        .scalarTypeAdapters(scalarTypeAdapters)
        .apolloStore(apolloStore)
        .responseFetcher(defaultResponseFetcher)
        .cacheHeaders(defaultCacheHeaders)
        .dispatcher(dispatcher)
        .logger(logger)
        .applicationInterceptors(applicationInterceptors)
        .applicationInterceptorFactories(applicationInterceptorFactories)
        .autoPersistedOperationsInterceptorFactory(autoPersistedOperationsInterceptorFactory)
        .tracker(tracker)
        .refetchQueries(emptyList())
        .refetchQueryNames(emptyList())
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .useHttpGetMethodForQueries(useHttpGetMethodForQueries)
        .useHttpGetMethodForPersistedQueries(useHttpGetMethodForPersistedQueries)
        .writeToNormalizedCacheAsynchronously(writeToNormalizedCacheAsynchronously)
        .build()
  }

  class Builder {
    var callFactory: Call.Factory? = null
    var serverUrl: HttpUrl? = null
    var httpCache: HttpCache? = null
    var apolloStore: ApolloStore = ApolloStore.emptyApolloStore
    var cacheFactory = absent<NormalizedCacheFactory<*>>()
    var cacheKeyResolver = absent<CacheKeyResolver>()
    var defaultHttpCachePolicy = HttpCachePolicy.NETWORK_ONLY
    var defaultResponseFetcher = ApolloResponseFetchers.CACHE_FIRST
    var defaultCacheHeaders = CacheHeaders.NONE
    val CustomScalarAdapters: MutableMap<CustomScalar, CustomScalarAdapter<*>> = LinkedHashMap()
    var dispatcher: Executor? = null
    var logger: Logger? = null
    val applicationInterceptors: MutableList<ApolloInterceptor> = ArrayList()
    val applicationInterceptorFactories: MutableList<ApolloInterceptorFactory> = ArrayList()
    var autoPersistedOperationsInterceptorFactory: ApolloInterceptorFactory? = null
    var enableAutoPersistedQueries = false
    var subscriptionManager: SubscriptionManager = NoOpSubscriptionManager()
    var enableAutoPersistedSubscriptions = false
    var subscriptionTransportFactory = absent<SubscriptionTransport.Factory>()
    var subscriptionConnectionParams: SubscriptionConnectionParamsProvider = SubscriptionConnectionParamsProvider.Const(
        SubscriptionConnectionParams())
    var subscriptionHeartbeatTimeout: Long = -1
    var useHttpGetMethodForQueries = false
    var useHttpGetMethodForPersistedQueries = false
    var writeToNormalizedCacheAsynchronously = false

    internal constructor()
    constructor(apolloClient: ApolloClient) {
      callFactory = apolloClient.httpCallFactory
      serverUrl = apolloClient.serverUrl
      httpCache = apolloClient.httpCache
      apolloStore = apolloClient.apolloStore
      defaultHttpCachePolicy = apolloClient.defaultHttpCachePolicy
      defaultResponseFetcher = apolloClient.defaultResponseFetcher
      defaultCacheHeaders = apolloClient.defaultCacheHeaders
      CustomScalarAdapters.putAll(apolloClient.scalarTypeAdapters.customScalarAdapters)
      dispatcher = apolloClient.dispatcher
      logger = apolloClient.logger.logger
      applicationInterceptors.addAll(apolloClient.applicationInterceptors)
      applicationInterceptorFactories.addAll(apolloClient.applicationInterceptorFactories)
      autoPersistedOperationsInterceptorFactory = apolloClient.autoPersistedOperationsInterceptorFactory
      enableAutoPersistedQueries = apolloClient.enableAutoPersistedQueries
      subscriptionManager = apolloClient.subscriptionManager
      useHttpGetMethodForQueries = apolloClient.useHttpGetMethodForQueries
      useHttpGetMethodForPersistedQueries = apolloClient.useHttpGetMethodForPersistedQueries
      writeToNormalizedCacheAsynchronously = apolloClient.writeToNormalizedCacheAsynchronously
    }

    /**
     * Set the [OkHttpClient] to use for making network requests.
     *
     * @param okHttpClient the client to use.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun okHttpClient(okHttpClient: OkHttpClient): Builder {
      return callFactory(okHttpClient)
    }

    /**
     * Set the custom call factory for creating [Call] instances.
     *
     * Note: Calling [.okHttpClient] automatically
     * sets this value.
     */
    fun callFactory(factory: Call.Factory): Builder {
      callFactory = factory
      return this
    }

    /**
     *
     * Set the API server's base url.
     *
     * @param serverUrl the url to set.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun serverUrl(serverUrl: HttpUrl): Builder {
      this.serverUrl = serverUrl
      return this
    }

    /**
     *
     * Set the API server's base url.
     *
     * @param serverUrl the url to set.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun serverUrl(serverUrl: String): Builder {
      this.serverUrl = HttpUrl.parse(serverUrl)
      return this
    }

    /**
     * Set the configuration to be used for request/response http cache.
     *
     * @param httpCache The to use for reading and writing cached response.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun httpCache(httpCache: HttpCache): Builder {
      this.httpCache = httpCache
      return this
    }
    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the [NormalizedCacheFactory] used to construct a [NormalizedCache].
     * @param keyResolver the [CacheKeyResolver] to use to normalize records
     * @param writeToCacheAsynchronously If true returning response data will not wait on the normalized cache write. This can
     * improve request performance, but means that subsequent requests are not guaranteed to hit the cache for data contained
     * in previously received requests.
     * @return The [Builder] object to be used for chaining method calls
     */
    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the [NormalizedCacheFactory] used to construct a [NormalizedCache].
     * @param keyResolver the [CacheKeyResolver] to use to normalize records
     * @return The [Builder] object to be used for chaining method calls
     */
    /**
     * Set the configuration to be used for normalized cache.
     *
     * @param normalizedCacheFactory the [NormalizedCacheFactory] used to construct a [NormalizedCache].
     * @return The [Builder] object to be used for chaining method calls
     */
    @JvmOverloads
    fun normalizedCache(normalizedCacheFactory: NormalizedCacheFactory<*>,
                        keyResolver: CacheKeyResolver = CacheKeyResolver.DEFAULT, writeToCacheAsynchronously: Boolean = false): Builder {
      cacheFactory = Optional.fromNullable(normalizedCacheFactory)
      cacheKeyResolver = Optional.fromNullable((keyResolver))
      writeToNormalizedCacheAsynchronously = writeToCacheAsynchronously
      return this
    }

    /**
     * Set the type adapter to use for serializing and de-serializing custom GraphQL scalar types.
     *
     * @param customScalar the scalar type to serialize/deserialize
     * @param customScalarAdapter the type adapter to use
     * @param <T> the value type
     * @return The [Builder] object to be used for chaining method calls
    </T> */
    fun <T> addCustomScalarAdapter(customScalar: CustomScalar,
                                   customScalarAdapter: CustomScalarAdapter<T>): Builder {
      CustomScalarAdapters[customScalar] = customScalarAdapter
      return this
    }

    /**
     * The #[Executor] to use for dispatching the requests.
     *
     * @return The [Builder] object to be used for chaining method calls
     */
    fun dispatcher(dispatcher: Executor): Builder {
      this.dispatcher = dispatcher
      return this
    }

    /**
     * Sets the http cache policy to be used as default for all GraphQL [Query] operations. Will be ignored for any [Mutation]
     * operations. By default http cache policy is set to [HttpCachePolicy.NETWORK_ONLY].
     *
     * @return The [Builder] object to be used for chaining method calls
     */
    fun defaultHttpCachePolicy(cachePolicy: HttpCachePolicy.Policy): Builder {
      defaultHttpCachePolicy = cachePolicy
      return this
    }

    /**
     * Set the default [CacheHeaders] strategy that will be passed to the [com.apollographql.apollo3.interceptor.FetchOptions]
     * used in each new [ApolloCall].
     *
     * @return The [Builder] object to be used for chaining method calls
     */
    fun defaultCacheHeaders(cacheHeaders: CacheHeaders): Builder {
      defaultCacheHeaders = cacheHeaders
      return this
    }

    /**
     * Set the default [ResponseFetcher] to be used with each new [ApolloCall].
     *
     * @return The [Builder] object to be used for chaining method calls
     */
    fun defaultResponseFetcher(defaultResponseFetcher: ResponseFetcher): Builder {
      this.defaultResponseFetcher = defaultResponseFetcher
      return this
    }

    /**
     * The [Logger] to use for logging purposes.
     *
     * @return The [Builder] object to be used for chaining method calls
     */
    fun logger(logger: Logger?): Builder {
      this.logger = logger
      return this
    }

    /**
     *
     * Adds an interceptor that observes the full span of each call: from before the connection is established until
     * after the response source is selected (either the server, cache or both). This method can be called multiple times for adding
     * multiple application interceptors.
     *
     *
     * Note: Interceptors will be called **in the order in which they are added to the list of interceptors** and
     * if any of the interceptors tries to short circuit the responses, then subsequent interceptors **won't** be called.
     *
     * @param interceptor Application level interceptor to add
     * @return The [Builder] object to be used for chaining method calls
     */
    fun addApplicationInterceptor(interceptor: ApolloInterceptor): Builder {
      applicationInterceptors.add(interceptor)
      return this
    }

    /**
     *
     * Adds an interceptorFactory that creates interceptors that observes the full span of each call: from before
     * the connection is established until after the response source is selected (either the server, cache or both). This method can be
     * called multiple times for adding multiple application interceptors.
     *
     *
     * Note: Interceptors will be called **in the order in which they are added to the list of interceptors** and
     * if any of the interceptors tries to short circuit the responses, then subsequent interceptors **won't** be called.
     *
     * @param interceptorFactory Application level interceptor to add
     * @return The [Builder] object to be used for chaining method calls
     */
    fun addApplicationInterceptorFactory(interceptorFactory: ApolloInterceptorFactory): Builder {
      applicationInterceptorFactories.add(interceptorFactory)
      return this
    }

    /**
     *
     * Sets the interceptor to use for auto persisted operations.
     *
     *
     * @param interceptorFactory interceptor to set
     * @return The [Builder] object to be used for chaining method calls
     */
    fun setAutoPersistedOperationsInterceptorFactory(interceptorFactory: ApolloInterceptorFactory?): Builder {
      autoPersistedOperationsInterceptorFactory = interceptorFactory
      return this
    }

    /**
     * @param enableAutoPersistedQueries True if ApolloClient should enable Automatic Persisted Queries support. Default: false.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean): Builder {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
      return this
    }

    /**
     *
     * Sets up subscription transport factory to be used for subscription server communication.
     *
     * See also: [ ]
     *
     * @param subscriptionTransportFactory transport layer to be used for subscriptions.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun subscriptionTransportFactory(subscriptionTransportFactory: SubscriptionTransport.Factory): Builder {
      this.subscriptionTransportFactory = of(subscriptionTransportFactory)
      return this
    }

    /**
     *
     * Sets up subscription connection parameters to be sent to the server when connection is established with subscription server
     *
     * @param connectionParams map of connection parameters to be sent
     * @return The [Builder] object to be used for chaining method calls
     */
    fun subscriptionConnectionParams(connectionParams: SubscriptionConnectionParams): Builder {
      subscriptionConnectionParams = SubscriptionConnectionParamsProvider.Const(connectionParams)
      return this
    }

    /**
     *
     * Sets up subscription connection parameters to be sent to the server when connection is established with subscription server
     *
     * @param provider connection parameters provider
     * @return The [Builder] object to be used for chaining method calls
     */
    fun subscriptionConnectionParams(provider: SubscriptionConnectionParamsProvider): Builder {
      subscriptionConnectionParams = provider
      return this
    }

    /**
     *
     * Sets up subscription heartbeat message timeout. Timeout for how long subscription manager should wait for a
     * keep-alive message from the subscription server before reconnect. **NOTE: will be ignored if server doesn't send keep-alive
     * messages.******. By default heartbeat timeout is disabled.
     *
     * @param timeout connection keep alive timeout. Min value is 10 secs.
     * @param timeUnit time unit
     * @return The [Builder] object to be used for chaining method calls
     */
    fun subscriptionHeartbeatTimeout(timeout: Long, timeUnit: TimeUnit): Builder {
      subscriptionHeartbeatTimeout = Math.max(timeUnit.toMillis(timeout), TimeUnit.SECONDS.toMillis(10))
      return this
    }

    /**
     * @param enableAutoPersistedSubscriptions True if ApolloClient should enable Automatic Persisted Subscriptions support. Default:
     * false.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun enableAutoPersistedSubscriptions(enableAutoPersistedSubscriptions: Boolean): Builder {
      this.enableAutoPersistedSubscriptions = enableAutoPersistedSubscriptions
      return this
    }

    /**
     * Sets flag whether GraphQL queries should be sent via HTTP GET requests.
     *
     * @param useHttpGetMethodForQueries `true` if HTTP GET requests should be used, `false` otherwise.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun useHttpGetMethodForQueries(useHttpGetMethodForQueries: Boolean): Builder {
      this.useHttpGetMethodForQueries = useHttpGetMethodForQueries
      return this
    }

    /**
     * Sets flag whether GraphQL Persisted queries should be sent via HTTP GET requests.
     *
     * @param useHttpGetMethodForPersistedQueries `true` if HTTP GET requests should be used, `false` otherwise.
     * @return The [Builder] object to be used for chaining method calls
     */
    fun useHttpGetMethodForPersistedQueries(useHttpGetMethodForPersistedQueries: Boolean): Builder {
      this.useHttpGetMethodForPersistedQueries = useHttpGetMethodForPersistedQueries
      return this
    }

    /**
     * Builds the [ApolloClient] instance using the configured values.
     *
     *
     * Note that if the [.dispatcher] is not called, then a default [Executor] is used.
     *
     * @return The configured [ApolloClient]
     */
    fun build(): ApolloClient {
      val apolloLogger = ApolloLogger(logger)
      var callFactory = callFactory
      if (callFactory == null) {
        callFactory = OkHttpClient()
      }
      val httpCache = httpCache
      if (httpCache != null) {
        callFactory = addHttpCacheInterceptorIfNeeded(callFactory, httpCache.interceptor())
      }
      var dispatcher = dispatcher
      if (dispatcher == null) {
        dispatcher = defaultDispatcher()
      }
      val customScalarAdapters = ResponseAdapterCache(Collections.unmodifiableMap(CustomScalarAdapters))
      var apolloStore = apolloStore
      val cacheFactory = cacheFactory
      val cacheKeyResolver = cacheKeyResolver
      if (cacheFactory.isPresent && cacheKeyResolver.isPresent) {
        val normalizedCache = cacheFactory.get().createChain()
        apolloStore = RealApolloStore(normalizedCache, cacheKeyResolver.get(), customScalarAdapters, apolloLogger)
      }
      var subscriptionManager = subscriptionManager
      val subscriptionTransportFactory = subscriptionTransportFactory
      if (subscriptionTransportFactory.isPresent) {
        subscriptionManager = RealSubscriptionManager(
            customScalarAdapters,
            subscriptionTransportFactory.get(),
            subscriptionConnectionParams,
            dispatcher,
            subscriptionHeartbeatTimeout,
            cacheKeyResolver.or(CacheKeyResolver.DEFAULT),
            enableAutoPersistedSubscriptions)
      }
      return ApolloClient(serverUrl,
          callFactory,
          httpCache,
          apolloStore,
          customScalarAdapters,
          dispatcher,
          defaultHttpCachePolicy,
          defaultResponseFetcher,
          defaultCacheHeaders,
          apolloLogger,
          Collections.unmodifiableList(applicationInterceptors),
          Collections.unmodifiableList(applicationInterceptorFactories),
          autoPersistedOperationsInterceptorFactory,
          enableAutoPersistedQueries,
          subscriptionManager,
          useHttpGetMethodForQueries,
          useHttpGetMethodForPersistedQueries,
          writeToNormalizedCacheAsynchronously)
    }

    private fun defaultDispatcher(): Executor {
      return ThreadPoolExecutor(0, Int.MAX_VALUE, 60, TimeUnit.SECONDS,
          SynchronousQueue()) { runnable -> Thread(runnable, "Apollo Dispatcher") }
    }

    companion object {
      private fun addHttpCacheInterceptorIfNeeded(callFactory: Call.Factory,
                                                  httpCacheInterceptor: Interceptor): Call.Factory {
        if (callFactory is OkHttpClient) {
          val client = callFactory
          for (interceptor in client.interceptors()) {
            if (interceptor.javaClass == httpCacheInterceptor.javaClass) {
              return callFactory
            }
          }
          return client.newBuilder().addInterceptor(httpCacheInterceptor).build()
        }
        return callFactory
      }
    }
  }

  companion object {
    @JvmStatic
    fun builder(): Builder {
      return Builder()
    }
  }

  init {
    require(!(!applicationInterceptorFactories.isEmpty() && !applicationInterceptors.isEmpty())) {
      ("You can either use applicationInterceptors or applicationInterceptorFactories "
          + "but not both at the same time.")
    }
    this.applicationInterceptors = applicationInterceptors
    this.applicationInterceptorFactories = applicationInterceptorFactories
    this.autoPersistedOperationsInterceptorFactory = autoPersistedOperationsInterceptorFactory
    this.enableAutoPersistedQueries = enableAutoPersistedQueries
    this.subscriptionManager = subscriptionManager
    this.useHttpGetMethodForQueries = useHttpGetMethodForQueries
    this.useHttpGetMethodForPersistedQueries = useHttpGetMethodForPersistedQueries
    this.writeToNormalizedCacheAsynchronously = writeToNormalizedCacheAsynchronously
  }
}
