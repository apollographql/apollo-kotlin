package com.apollographql.apollo

import com.apollographql.apollo.annotations.ApolloDeprecatedSince
import com.apollographql.apollo.annotations.ApolloExperimental
import com.apollographql.apollo.annotations.ApolloInternal
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.CustomScalarType
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.ExecutionOptions
import com.apollographql.apollo.api.MutableExecutionOptions
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.api.http.HttpHeader
import com.apollographql.apollo.api.http.HttpMethod
import com.apollographql.apollo.exception.ApolloHttpException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo.interceptor.DefaultInterceptorChain
import com.apollographql.apollo.interceptor.NetworkInterceptor
import com.apollographql.apollo.interceptor.RetryOnErrorInterceptor
import com.apollographql.apollo.internal.ApolloClientListener
import com.apollographql.apollo.internal.defaultDispatcher
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.network.http.BatchingHttpInterceptor
import com.apollographql.apollo.network.http.HttpEngine
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpNetworkTransport
import com.apollographql.apollo.network.ws.WebSocketEngine
import com.apollographql.apollo.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo.network.ws.WsProtocol
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import okio.Closeable
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
 *
 * Use [ApolloClient.Builder] to create a new [ApolloClient]:
 *
 * ```
 * val apolloClient = ApolloClient.Builder()
 *     .serverUrl("https://example.com/graphql")
 *     .build()
 *
 * val response = apolloClient.query(MyQuery()).execute()
 * if (response.data != null) {
 *   // Handle (potentially partial) data
 * } else {
 *   // Something wrong happened
 *   if (response.exception != null) {
 *     // Handle fetch errors
 *   } else {
 *     // Handle GraphQL errors in response.errors
 *   }
 * }
 * ```
 *
 * On native targets, [ApolloClient.close] must be called to release resources when not in use anymore.
 */
class ApolloClient
private constructor(
    private val builder: Builder,
) : ExecutionOptions, Closeable {
  private val concurrencyInfo: ConcurrencyInfo
  val networkTransport: NetworkTransport
  val subscriptionNetworkTransport: NetworkTransport
  val interceptors: List<ApolloInterceptor> = builder.interceptors
  val customScalarAdapters: CustomScalarAdapters = builder.customScalarAdapters
  private val retryOnError: ((ApolloRequest<*>) -> Boolean)? = builder.retryOnError
  private val retryOnErrorInterceptor: ApolloInterceptor? = builder.retryOnErrorInterceptor
  private val failFastIfOffline = builder.failFastIfOffline
  private val listeners = builder.listeners

  override val executionContext: ExecutionContext = builder.executionContext
  override val httpMethod: HttpMethod? = builder.httpMethod
  override val httpHeaders: List<HttpHeader>? = builder.httpHeaders
  override val sendApqExtensions: Boolean? = builder.sendApqExtensions
  override val sendDocument: Boolean? = builder.sendDocument
  override val enableAutoPersistedQueries: Boolean? = builder.enableAutoPersistedQueries
  override val canBeBatched: Boolean? = builder.canBeBatched

  init {
    networkTransport = if (builder.networkTransport != null) {
      check(builder.httpServerUrl == null) {
        "Apollo: 'httpServerUrl' has no effect if 'networkTransport' is set"
      }
      check(builder.httpEngine == null) {
        "Apollo: 'httpEngine' has no effect if 'networkTransport' is set"
      }
      check(builder.httpInterceptors.isEmpty()) {
        "Apollo: 'addHttpInterceptor' has no effect if 'networkTransport' is set"
      }
      check(builder.httpExposeErrorBody == null) {
        "Apollo: 'httpExposeErrorBody' has no effect if 'networkTransport' is set"
      }
      builder.networkTransport!!
    } else {
      check(builder.httpServerUrl != null) {
        "Apollo: 'serverUrl' is required"
      }
      HttpNetworkTransport.Builder()
          .serverUrl(builder.httpServerUrl!!)
          .apply {
            if (builder.httpEngine != null) {
              httpEngine(builder.httpEngine!!)
            }
            if (builder.httpExposeErrorBody != null) {
              exposeErrorBody(builder.httpExposeErrorBody!!)
            }
          }
          .interceptors(builder.httpInterceptors)
          .build()
    }

    subscriptionNetworkTransport = if (builder.subscriptionNetworkTransport != null) {
      check(builder.webSocketServerUrl == null) {
        "Apollo: 'webSocketServerUrl' has no effect if 'subscriptionNetworkTransport' is set"
      }
      check(builder.webSocketEngine == null) {
        "Apollo: 'webSocketEngine' has no effect if 'subscriptionNetworkTransport' is set"
      }
      check(builder.webSocketIdleTimeoutMillis == null) {
        "Apollo: 'webSocketIdleTimeoutMillis' has no effect if 'subscriptionNetworkTransport' is set"
      }
      check(builder.wsProtocolFactory == null) {
        "Apollo: 'wsProtocolFactory' has no effect if 'subscriptionNetworkTransport' is set"
      }
      check(builder.webSocketReopenWhen == null) {
        "Apollo: 'webSocketReopenWhen' has no effect if 'subscriptionNetworkTransport' is set"
      }
      check(builder.webSocketReopenServerUrl == null) {
        "Apollo: 'webSocketReopenServerUrl' has no effect if 'subscriptionNetworkTransport' is set"
      }
      builder.subscriptionNetworkTransport!!
    } else {
      val url = builder.webSocketServerUrl ?: builder.httpServerUrl
      if (url == null) {
        // Fallback to the regular [NetworkTransport]. This is unlikely to work but chances are
        // that the user is not going to use subscription, so it's better than failing
        networkTransport
      } else {
        WebSocketNetworkTransport.Builder()
            .serverUrl(url)
            .apply {
              if (builder.webSocketEngine != null) {
                webSocketEngine(builder.webSocketEngine!!)
              }
              if (builder.webSocketIdleTimeoutMillis != null) {
                idleTimeoutMillis(builder.webSocketIdleTimeoutMillis!!)
              }
              if (builder.wsProtocolFactory != null) {
                protocol(builder.wsProtocolFactory!!)
              }
              if (builder.webSocketReopenWhen != null) {
                reopenWhen(builder.webSocketReopenWhen)
              }
              if (builder.webSocketReopenServerUrl != null) {
                serverUrl(builder.webSocketReopenServerUrl)
              }
            }
            .build()
      }
    }

    val dispatcher = builder.dispatcher ?: defaultDispatcher
    concurrencyInfo = ConcurrencyInfo(
        dispatcher,
        CoroutineScope(dispatcher)
    )
  }

  /**
   * Creates a new [ApolloCall] for the given [Query].
   */
  fun <D : Query.Data> query(query: Query<D>): ApolloCall<D> {
    return ApolloCall(this, query)
  }

  /**
   * Creates a new [ApolloCall] for the given [Mutation].
   */
  fun <D : Mutation.Data> mutation(mutation: Mutation<D>): ApolloCall<D> {
    return ApolloCall(this, mutation)
  }

  /**
   * Creates a new [ApolloCall] for the given [Subscription].
   */
  fun <D : Subscription.Data> subscription(subscription: Subscription<D>): ApolloCall<D> {
    return ApolloCall(this, subscription)
  }

  @Deprecated("Use close() instead", ReplaceWith("close()"), level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun dispose() {
    close()
  }

  /**
   * Disposes resources held by this [ApolloClient]. On JVM platforms, resources are ultimately garbage collected but calling [close] is necessary
   * on other platform or to reclaim those resources earlier.
   */
  override fun close() {
    concurrencyInfo.coroutineScope.cancel()
    networkTransport.dispose()
    subscriptionNetworkTransport.dispose()
  }

  private val networkInterceptor = NetworkInterceptor(
      networkTransport = networkTransport,
      subscriptionNetworkTransport = subscriptionNetworkTransport,
  )

  /**
   * Low level API to execute the given [apolloRequest] and return a [Flow].
   *
   * Prefer [query], [mutation] or [subscription] when possible.
   *
   * For simple queries, the returned [Flow] contains only one element.
   * For more advanced use cases like watchers or subscriptions, it may contain any number of elements and never
   * finish. You can cancel the corresponding coroutine to terminate the [Flow] in this case.
   *
   * @see query
   * @see mutation
   * @see subscription
   */
  fun <D : Operation.Data> executeAsFlow(
      apolloRequest: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>> {
    return executeAsFlowInternal(apolloRequest, false)
  }

  internal fun <D : Operation.Data> executeAsFlowInternal(
      apolloRequest: ApolloRequest<D>,
      throwing: Boolean,
  ): Flow<ApolloResponse<D>> {
    val flow = channelFlow {
      listeners.forEach {
        it.requestStarted(apolloRequest)
      }

      try {
        withContext(concurrencyInfo.dispatcher) {
          apolloResponses(apolloRequest, throwing).collect {
            send(it)
          }
        }
      } finally {
        listeners.forEach {
          it.requestCompleted(apolloRequest)
        }
      }
    }

    return flow
        /**
         * The `Unconfined` dispatcher is needed to let the IdlingResource monitor collection synchronously. Without this, `channelFlow` dispatches and the current stack frame terminates with the IdlingResource still being idle.
         *
         * See [idlingResourceSingleQuery](https://github.com/apollographql/apollo-kotlin/blob/215a845f38bcdfc9c72ffc7c58d077ac54e297bb/tests/idling-resource/src/test/java/IdlingResourceTest.kt#L33).
         * See https://slack-chats.kotlinlang.org/t/16714036/can-i-have-a-channelflow-that-does-not-dispatch-until-after-#d48ebff5-676e-4109-8dd5-b2320245faa3.
         */
        .flowOn(Dispatchers.Unconfined)
        /**
         * Default to [Channel.UNLIMITED] so as not to lose any item. If a consumer is very slow, the channel may grow boundless. The caller should call [buffer] and change the default in those cases.
         */
        .buffer(Channel.UNLIMITED)
  }

  internal fun <D : Operation.Data> apolloResponses(
      apolloRequest: ApolloRequest<D>,
      throwing: Boolean,
  ): Flow<ApolloResponse<D>> {
    val apolloClient = this
    val request = apolloRequest.newBuilder().apply {
      executionContext(concurrencyInfo + customScalarAdapters + apolloClient.executionContext + executionContext)
      httpMethod(httpMethod ?: apolloClient.httpMethod)
      sendApqExtensions(sendApqExtensions ?: apolloClient.sendApqExtensions)
      sendDocument(sendDocument ?: apolloClient.sendDocument)
      enableAutoPersistedQueries(enableAutoPersistedQueries ?: apolloClient.enableAutoPersistedQueries)

      val headers = buildList {
        if (ignoreApolloClientHttpHeaders != true) {
          addAll(apolloClient.httpHeaders.orEmpty())
        }
        addAll(httpHeaders.orEmpty())
      }
      httpHeaders(headers)

      val canBeBatched = canBeBatched ?: apolloClient.canBeBatched
      if (canBeBatched != null) {
        // Because batching is handled at the HTTP level, move the information to HTTP headers
        // canBeBatched(apolloRequest.canBeBatched)
        addHttpHeader(ExecutionOptions.CAN_BE_BATCHED, canBeBatched.toString())
      }

      retryOnError(retryOnError ?: apolloClient.retryOnError?.invoke(apolloRequest))
      failFastIfOffline(failFastIfOffline ?: apolloClient.failFastIfOffline)
    }.build()

    val allInterceptors = buildList {
      addAll(interceptors)
      add(retryOnErrorInterceptor ?: RetryOnErrorInterceptor())
      add(networkInterceptor)
    }
    return DefaultInterceptorChain(allInterceptors, 0)
        .proceed(request)
        .let {
          if (throwing) {
            it.onEach { response ->
              if (response.exception != null) {
                throw response.exception!!
              }
            }
          } else {
            it
          }
        }
  }

  /**
   * Creates a new [Builder] from this [ApolloClient].
   */
  fun newBuilder(): Builder {
    return builder.copy()
  }

  /**
   * A Builder used to create instances of [ApolloClient].
   */
  class Builder : MutableExecutionOptions<Builder> {
    private val _customScalarAdaptersBuilder = CustomScalarAdapters.Builder()
    val customScalarAdapters: CustomScalarAdapters get() = _customScalarAdaptersBuilder.build()

    private val _interceptors: MutableList<ApolloInterceptor> = mutableListOf()
    val interceptors: List<ApolloInterceptor> = _interceptors

    private val _httpInterceptors: MutableList<HttpInterceptor> = mutableListOf()
    val httpInterceptors: List<HttpInterceptor> = _httpInterceptors

    private val _listeners: MutableList<ApolloClientListener> = mutableListOf()
    internal val listeners: List<ApolloClientListener> = _listeners

    override var executionContext: ExecutionContext = ExecutionContext.Empty
      private set
    override var httpMethod: HttpMethod? = null
      private set
    override var httpHeaders: List<HttpHeader>? = null
      private set
    override var sendApqExtensions: Boolean? = null
      private set
    override var sendDocument: Boolean? = null
      private set
    override var enableAutoPersistedQueries: Boolean? = null
      private set
    override var canBeBatched: Boolean? = null
      private set
    var networkTransport: NetworkTransport? = null
      private set
    var subscriptionNetworkTransport: NetworkTransport? = null
      private set
    var dispatcher: CoroutineDispatcher? = null
      private set
    var httpServerUrl: String? = null
      private set
    var httpEngine: HttpEngine? = null
      private set
    var webSocketServerUrl: String? = null
      private set
    var webSocketIdleTimeoutMillis: Long? = null
      private set
    var wsProtocolFactory: WsProtocol.Factory? = null
      private set
    var httpExposeErrorBody: Boolean? = null
      private set
    var webSocketEngine: WebSocketEngine? = null
      private set
    var webSocketReopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)? = null
      private set
    var webSocketReopenServerUrl: (suspend () -> String)? = null
      private set

    @ApolloExperimental
    var retryOnError: ((ApolloRequest<*>) -> Boolean)? = null
      private set

    @ApolloExperimental
    var retryOnErrorInterceptor: ApolloInterceptor? = null
      private set

    @ApolloExperimental
    var failFastIfOffline: Boolean? = null
      private set

    /**
     * Whether to fail fast if the device is offline.
     * Requires setting an interceptor that is aware of the network state with [retryOnErrorInterceptor].
     *
     * @see [retryOnErrorInterceptor]
     * @see [com.apollographql.apollo.network.NetworkMonitor]
     */
    @ApolloExperimental
    fun failFastIfOffline(failFastIfOffline: Boolean?): Builder = apply {
      this.failFastIfOffline = failFastIfOffline
    }

    /**
     * Configures the [retryOnError] default if [ApolloRequest.retryOnError] is not set.
     *
     * For an example, to retry all subscriptions by default:
     * ```
     * val apolloClient = ApolloClient.Builder()
     *     .retryOnError { it.operation is Subscription }
     *     .serverUrl("...")
     *     .build()
     * ```
     *
     * @param retryOnError a function called if [ApolloRequest.retryOnError] is `null` and returns a default value. Pass `null` to use the default `{ false }`
     * @see [ApolloRequest.retryOnError], [ApolloCall.retryOnError]
     */
    @ApolloExperimental
    fun retryOnError(retryOnError: ((ApolloRequest<*>) -> Boolean)?): Builder = apply {
      this.retryOnError = retryOnError
    }

    /**
     * Sets the [ApolloInterceptor] used to retry or fail fast a request. The interceptor may use [ApolloRequest.retryOnError]
     * and [ApolloRequest.failFastIfOffline].
     * The interceptor is also responsible for allocating a new [ApolloRequest.requestUuid] on retries if needed.
     *
     * By default [ApolloClient] uses a best effort interceptor that is not aware about network state, uses exponential backoff
     * and ignores [ApolloRequest.failFastIfOffline].
     *
     * Use [RetryOnErrorInterceptor] to add network state awareness:
     *
     * ```
     * apolloClient = ApolloClient.Builder()
     *                 .serverUrl("https://...")
     *                 .retryOnErrorInterceptor(RetryOnErrorInterceptor(NetworkMonitor(context)))
     *                 .build()
     * ```
     *
     * @param retryOnErrorInterceptor the [ApolloInterceptor] to use for retrying or `null` to use the default interceptor.
     *
     * @see [RetryOnErrorInterceptor]
     * @see [ApolloRequest.retryOnError]
     * @see [ApolloRequest.failFastIfOffline]
     */
    @ApolloExperimental
    fun retryOnErrorInterceptor(retryOnErrorInterceptor: ApolloInterceptor?) = apply {
      this.retryOnErrorInterceptor = retryOnErrorInterceptor
    }

    /**
     * Configures the [HttpMethod] to use.
     *
     * @param httpMethod the [HttpMethod] to use or `null` to use the default [HttpMethod.Post].
     *
     * @see [com.apollographql.apollo.api.http.DefaultHttpRequestComposer]
     */
    override fun httpMethod(httpMethod: HttpMethod?): Builder = apply {
      this.httpMethod = httpMethod
    }

    /**
     * Configures the [HttpHeader]s to use. These headers are added with the [ApolloCall] headers.
     *
     * @see [ApolloCall.httpHeaders]
     * @see [com.apollographql.apollo.api.http.DefaultHttpRequestComposer]
     */
    override fun httpHeaders(httpHeaders: List<HttpHeader>?): Builder = apply {
      this.httpHeaders = httpHeaders
    }

    /**
     * Adds a [HttpHeader] to the [ApolloClient] headers. The [ApolloClient] headers are added to the [ApolloCall] headers.
     *
     * @see [ApolloCall.httpHeaders]
     * @see [com.apollographql.apollo.api.http.DefaultHttpRequestComposer]
     */
    override fun addHttpHeader(name: String, value: String): Builder = apply {
      this.httpHeaders = this.httpHeaders.orEmpty() + HttpHeader(name, value)
    }

    /**
     * Whether to send the Auto Persisted Queries ([APQs](https://www.apollographql.com/docs/apollo-server/performance/apq/) extensions.
     *
     * This is the low level API used by [com.apollographql.apollo.api.http.DefaultHttpRequestComposer] to determine whether to send the APQ extensions:
     *
     * ```json
     * {
     *   "query": "{ __typename }"
     *   "extensions": {"persistedQuery":{"version":1,"sha256Hash":"ecf4edb46db40b5132295c0291d62fb65d6759a9eedfa4d5d612dd5ec54a6b38"}}
     * }
     * ```
     *
     * To configure APQs in general, including retry behaviour, use [autoPersistedQueries] and [enableAutoPersistedQueries].
     *
     * @see autoPersistedQueries
     * @see enableAutoPersistedQueries
     * @see com.apollographql.apollo.api.http.DefaultHttpRequestComposer
     */
    override fun sendApqExtensions(sendApqExtensions: Boolean?): Builder = apply {
      this.sendApqExtensions = sendApqExtensions
    }

    /**
     * Whether to send the [GraphQL Document](https://spec.graphql.org/October2021/#Document).
     *
     * This is the low level API used by [com.apollographql.apollo.api.http.DefaultHttpRequestComposer] to determine whether to send the "query" GraphQL document.
     *
     * Set [sendDocument] to `false` if your server supports [persisted queries](https://www.apollographql.com/docs/kotlin/advanced/persisted-queries/) and
     * can execute an operation base on an id instead.
     *
     * To configure APQs in general, including retry behaviour, use [autoPersistedQueries] and [enableAutoPersistedQueries].
     *
     * @see autoPersistedQueries
     * @see enableAutoPersistedQueries
     * @see com.apollographql.apollo.api.http.DefaultHttpRequestComposer
     */
    override fun sendDocument(sendDocument: Boolean?): Builder = apply {
      this.sendDocument = sendDocument
    }

    /**
     * Whether to enable Auto Persisted Queries ([APQs](https://www.apollographql.com/docs/apollo-server/performance/apq/) for this operation.
     *
     * APQs may retry the request if the server is sent an unknown id.
     *
     * @see autoPersistedQueries
     */
    override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): Builder = apply {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
    }

    /**
     * Whether this operation can be [batched](https://www.apollographql.com/docs/router/executing-operations/query-batching/).
     *
     * @see httpBatching
     */
    override fun canBeBatched(canBeBatched: Boolean?): Builder = apply {
      this.canBeBatched = canBeBatched
    }

    /**
     * The http:// or https:// url of the GraphQL server.
     *
     * This is the same as [httpServerUrl].
     *
     * This is a convenience function that configures the underlying [HttpNetworkTransport]. See also [networkTransport] for more customization.
     *
     * @see networkTransport
     */
    fun serverUrl(serverUrl: String) = apply {
      httpServerUrl = serverUrl
    }

    /**
     * The http:// or https:// url of the GraphQL server.
     *
     * This is the same as [serverUrl].
     *
     * This is a convenience function that configures the underlying [HttpNetworkTransport]. See also [networkTransport] for more customization.
     *
     * @see networkTransport
     */
    fun httpServerUrl(httpServerUrl: String?) = apply {
      this.httpServerUrl = httpServerUrl
    }

    /**
     * The [HttpEngine] to use for HTTP requests.
     *
     * This is a convenience function that configures the underlying [HttpNetworkTransport]. See also [networkTransport] for more customization.
     *
     * @see networkTransport
     */
    fun httpEngine(httpEngine: HttpEngine?) = apply {
      this.httpEngine = httpEngine
    }

    /**
     * Configures whether to expose the error body in [ApolloHttpException].
     *
     * If you're setting this to `true`, you **must** read [ApolloResponse.exception] and close the body explicitly in case of an [ApolloHttpException]
     * to avoid sockets and other resources leaking.
     *
     * This is a convenience function that configures the underlying [HttpNetworkTransport]. See also [networkTransport] for more customization.
     *
     * @param httpExposeErrorBody whether to expose the error body or `null` to use the `false` default.
     */
    fun httpExposeErrorBody(httpExposeErrorBody: Boolean?) = apply {
      this.httpExposeErrorBody = httpExposeErrorBody
    }

    /**
     * Adds [httpInterceptor] to the list of HTTP interceptors.
     *
     * This is a convenience function that configures the underlying [HttpNetworkTransport]. See also [networkTransport] for more customization.
     *
     * @see networkTransport
     */
    fun httpInterceptors(httpInterceptors: List<HttpInterceptor>) = apply {
      _httpInterceptors.clear()
      _httpInterceptors.addAll(httpInterceptors)
    }

    /**
     * Adds [httpInterceptor] to the list of HTTP interceptors
     *
     * This is a convenience function that configures the underlying [HttpNetworkTransport]. See also [networkTransport] for more customization.
     *
     * @see networkTransport
     */
    fun addHttpInterceptor(httpInterceptor: HttpInterceptor) = apply {
      _httpInterceptors += httpInterceptor
    }

    /**
     * Removes [httpInterceptor] from the list of HTTP interceptors.
     */
    fun removeHttpInterceptor(httpInterceptor: HttpInterceptor) = apply {
      _httpInterceptors -= httpInterceptor
    }

    /**
     * The url of the GraphQL server used for WebSockets
     * Use this function or webSocketServerUrl((suspend () -> String)) but not both.
     *
     * This is a convenience function that configures the underlying [WebSocketNetworkTransport]. See also [subscriptionNetworkTransport] for more customization.
     *
     * @see subscriptionNetworkTransport
     */
    fun webSocketServerUrl(webSocketServerUrl: String?) = apply {
      this.webSocketServerUrl = webSocketServerUrl
    }

    /**
     * Configure dynamically the url of the GraphQL server used for WebSockets.
     * Use this function or webSocketServerUrl(String) but not both.
     *
     * @param webSocketServerUrl a function returning the new server URL.
     * This function will be called every time a WebSocket is opened. For example, you can use it to update your
     * auth credentials in case of an unauthorized error.
     *
     * It is a suspending function, so it can be used to introduce delay before setting the new server URL.
     *
     * This is a convenience function that configures the underlying [WebSocketNetworkTransport]. See also [subscriptionNetworkTransport] for more customization.
     *
     * @see subscriptionNetworkTransport
     */
    fun webSocketServerUrl(webSocketServerUrl: (suspend () -> String)?) = apply {
      this.webSocketReopenServerUrl = webSocketServerUrl
    }

    /**
     * The timeout after which an inactive WebSocket will be closed
     *
     * This is a convenience function that configures the underlying [WebSocketNetworkTransport]. See also [subscriptionNetworkTransport] for more customization.
     *
     * @param webSocketIdleTimeoutMillis the timeout in milliseconds or null to use the `60_000` default.
     *
     * @see subscriptionNetworkTransport
     */
    fun webSocketIdleTimeoutMillis(webSocketIdleTimeoutMillis: Long?) = apply {
      this.webSocketIdleTimeoutMillis = webSocketIdleTimeoutMillis
    }

    /**
     * The [WsProtocol.Factory] to use for websockets
     *
     * This is a convenience function that configures the underlying [WebSocketNetworkTransport]. See also [subscriptionNetworkTransport] for more customization.
     *
     * @see subscriptionNetworkTransport
     */
    fun wsProtocol(wsProtocolFactory: WsProtocol.Factory?) = apply {
      this.wsProtocolFactory = wsProtocolFactory
    }

    /**
     * The [WebSocketEngine] to use for WebSocket requests
     *
     * This is a convenience function that configures the underlying [WebSocketNetworkTransport]. See also [subscriptionNetworkTransport] for more customization.
     *
     * @see subscriptionNetworkTransport
     */
    fun webSocketEngine(webSocketEngine: WebSocketEngine?) = apply {
      this.webSocketEngine = webSocketEngine
    }

    /**
     * Configure the [WebSocketNetworkTransport] to reopen the websocket automatically when a network error
     * happens
     *
     * @param webSocketReopenWhen a function taking the error and attempt index (starting from zero) as parameters
     * and returning 'true' to reopen automatically or 'false' to forward the error to all listening [Flow].
     *
     * It is a suspending function, so it can be used to introduce delay before retry (e.g. backoff strategy).
     * attempt is reset after a successful connection.
     *
     * This is a convenience function that configures the underlying [WebSocketNetworkTransport]. See also [subscriptionNetworkTransport] for more customization.
     *
     * @see subscriptionNetworkTransport
     */
    fun webSocketReopenWhen(webSocketReopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)?) = apply {
      this.webSocketReopenWhen = webSocketReopenWhen
    }

    /**
     * Configures the [NetworkTransport] to use for queries and mutations.
     *
     * By default, an instance of [HttpNetworkTransport] is created.
     *
     * @see HttpNetworkTransport
     */
    fun networkTransport(networkTransport: NetworkTransport?) = apply {
      this.networkTransport = networkTransport
    }

    /**
     * Configures the [NetworkTransport] to use for queries and mutations.
     *
     * By default, an instance of [WebSocketNetworkTransport] using [graphql-ws](https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md) is created.
     *
     * @see WebSocketNetworkTransport
     */
    fun subscriptionNetworkTransport(subscriptionNetworkTransport: NetworkTransport?) = apply {
      this.subscriptionNetworkTransport = subscriptionNetworkTransport
    }

    /**
     * Configures the [CustomScalarAdapters].
     *
     * [Custom scalars](https://www.apollographql.com/docs/apollo-server/schema/custom-scalars/) allow schema designers to extend the GraphQL type
     * system with custom types such as `Date` or `Long`.
     *
     * See [the Apollo Kotlin documentation page](https://www.apollographql.com/docs/kotlin/essentials/custom-scalars/) and [scalars.graphql.org](https://scalars.graphql.org/) for more information.
     */
    fun customScalarAdapters(customScalarAdapters: CustomScalarAdapters) = apply {
      _customScalarAdaptersBuilder.clear()
      _customScalarAdaptersBuilder.addAll(customScalarAdapters)
    }

    /**
     * Adds the given [customScalarAdapter] to this [ApolloClient].
     *
     * [Custom scalars](https://www.apollographql.com/docs/apollo-server/schema/custom-scalars/) allow schema designers to extend the GraphQL type
     * system with custom types such as `Date` or `Long`.
     *
     * See [the Apollo Kotlin documentation page](https://www.apollographql.com/docs/kotlin/essentials/custom-scalars/) and [scalars.graphql.org](https://scalars.graphql.org/) for more information.

     * @param customScalarType a generated [CustomScalarType]. Every GraphQL custom scalar has a
     * generated class with a static `type` property. For an example, for a `Date` custom scalar,
     * you can use `com.example.Date.type`
     * @param customScalarAdapter the [Adapter] to use for this custom scalar.
     */
    fun <T> addCustomScalarAdapter(customScalarType: CustomScalarType, customScalarAdapter: Adapter<T>) = apply {
      _customScalarAdaptersBuilder.add(customScalarType, customScalarAdapter)
    }

    @ApolloInternal
    fun addListener(listener: ApolloClientListener) = apply {
      _listeners.add(listener)
    }

    @ApolloInternal
    fun listeners(listeners: List<ApolloClientListener>) = apply {
      _listeners.clear()
      _listeners.addAll(listeners)
    }

    /**
     * Adds an [ApolloInterceptor] to this [ApolloClient].
     *
     * [ApolloInterceptor]s monitor, rewrite and retry an [ApolloCall]. Internally, [ApolloInterceptor] is used for features
     * such as normalized cache and auto persisted queries. [ApolloClient] also inserts a terminating [ApolloInterceptor] that
     * executes the request.
     *
     * **The order is important**. The [ApolloInterceptor]s are executed in the order they are added. Because cache and APQs also
     * use interceptors, the order of the cache/APQs configuration also influences the final interceptor list.
     */
    fun addInterceptor(interceptor: ApolloInterceptor) = apply {
      _interceptors.add(interceptor)
    }

    /**
     * Removes an [ApolloInterceptor] from this [ApolloClient].
     *
     * **The order is important**. This method removes the first occurrence of the [ApolloInterceptor] in the list.
     */
    fun removeInterceptor(interceptor: ApolloInterceptor) = apply {
      _interceptors.remove(interceptor)
    }

    /**
     * Adds several [ApolloInterceptor]s to this [ApolloClient].
     *
     * [ApolloInterceptor]s monitor, rewrite and retry an [ApolloCall]. Internally, [ApolloInterceptor] is used for features
     * such as normalized cache and auto persisted queries. [ApolloClient] also inserts a terminating [ApolloInterceptor] that
     * executes the request.
     *
     * **The order is important**. The [ApolloInterceptor]s are executed in the order they are added. Because cache and APQs also
     * use interceptors, the order of the cache/APQs configuration also influences the final interceptor list.
     */
    @Deprecated("Use addInterceptor() or interceptors()")
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
    fun addInterceptors(interceptors: List<ApolloInterceptor>) = apply {
      this._interceptors += interceptors
    }

    /**
     * Sets the [ApolloInterceptor]s on this [ApolloClient].
     *
     * [ApolloInterceptor]s monitor, rewrite and retry an [ApolloCall]. Internally, [ApolloInterceptor] is used for features
     * such as normalized cache and auto persisted queries. [ApolloClient] also inserts a terminating [ApolloInterceptor] that
     * executes the request.
     *
     * **The order is important**. The [ApolloInterceptor]s are executed in the order they are added. Because cache and APQs also
     * use interceptors, the order of the cache/APQs configuration also influences the final interceptor list.
     */
    fun interceptors(interceptors: List<ApolloInterceptor>) = apply {
      this._interceptors.clear()
      this._interceptors += interceptors
    }

    /**
     * Sets the [CoroutineDispatcher] that the return value of [ApolloCall.toFlow] will flow on.
     *
     * By default, [ApolloClient] uses [kotlinx.coroutines.Dispatchers.IO] for JVM/native targets and [kotlinx.coroutines.Dispatchers.Default] for JS and other targets.
     *
     * @see [ApolloCall.toFlow]
     */
    fun dispatcher(dispatcher: CoroutineDispatcher?) = apply {
      this.dispatcher = dispatcher
    }

    override fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun executionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = executionContext
    }

    /**
     * Configures auto persisted queries.
     *
     * @param httpMethodForHashedQueries the [HttpMethod] to use for the initial hashed query that does not send the actual Graphql document.
     * [HttpMethod.Get] allows to use caching when available while [HttpMethod.Post] usually allows bigger document sizes. Mutations are
     * always sent using [HttpMethod.Post] regardless of this setting.
     * Default: [HttpMethod.Get]
     *
     * @param httpMethodForDocumentQueries the [HttpMethod] to use for the follow-up query that sends the full document if the initial
     * hashed query was not found. Mutations are always sent using [HttpMethod.Post] regardless of this setting.
     * Default: [HttpMethod.Post]
     *
     * @param enableByDefault whether to enable Auto Persisted Queries by default. You can later use
     * [ApolloCall.enableAutoPersistedQueries] on to enable/disable them on individual calls.
     */
    @JvmOverloads
    fun autoPersistedQueries(
        httpMethodForHashedQueries: HttpMethod = HttpMethod.Get,
        httpMethodForDocumentQueries: HttpMethod = HttpMethod.Post,
        enableByDefault: Boolean = true,
    ) = apply {
      _interceptors.removeAll { it is AutoPersistedQueryInterceptor }
      addInterceptor(
          AutoPersistedQueryInterceptor(
              httpMethodForHashedQueries,
              httpMethodForDocumentQueries
          )
      )
      enableAutoPersistedQueries(enableByDefault)
    }

    /**
     * Batch HTTP queries to execute multiple at once.
     * This reduces the number of HTTP round trips at the price of increased latency as
     * every request in the batch is now as slow as the slowest one.
     * Some servers might have a per-HTTP-call cache making it faster to resolve 1 big array
     * of n queries compared to resolving the n queries separately.
     *
     * See also [BatchingHttpInterceptor]
     *
     * @param batchIntervalMillis the interval between two batches
     * @param maxBatchSize always send the batch when this threshold is reached
     */
    @JvmOverloads
    fun httpBatching(
        batchIntervalMillis: Long = 10,
        maxBatchSize: Int = 10,
        enableByDefault: Boolean = true,
    ) = apply {
      _httpInterceptors.removeAll { it is BatchingHttpInterceptor }
      addHttpInterceptor(BatchingHttpInterceptor(batchIntervalMillis, maxBatchSize))
      canBeBatched(enableByDefault)
    }

    /**
     * Creates an [ApolloClient] from this [Builder]
     */
    fun build(): ApolloClient {
      return ApolloClient(
          this.copy()
      )
    }

    fun copy(): Builder {
      return Builder()
          .customScalarAdapters(_customScalarAdaptersBuilder.build())
          .interceptors(interceptors)
          .dispatcher(dispatcher)
          .executionContext(executionContext)
          .httpMethod(httpMethod)
          .httpHeaders(httpHeaders)
          .httpServerUrl(httpServerUrl)
          .httpEngine(httpEngine)
          .httpExposeErrorBody(httpExposeErrorBody)
          .httpInterceptors(httpInterceptors)
          .sendApqExtensions(sendApqExtensions)
          .sendDocument(sendDocument)
          .enableAutoPersistedQueries(enableAutoPersistedQueries)
          .canBeBatched(canBeBatched)
          .networkTransport(networkTransport)
          .subscriptionNetworkTransport(subscriptionNetworkTransport)
          .webSocketServerUrl(webSocketServerUrl)
          .webSocketServerUrl(webSocketReopenServerUrl)
          .webSocketEngine(webSocketEngine)
          .webSocketReopenWhen(webSocketReopenWhen)
          .webSocketIdleTimeoutMillis(webSocketIdleTimeoutMillis)
          .wsProtocol(wsProtocolFactory)
          .retryOnError(retryOnError)
          .retryOnErrorInterceptor(retryOnErrorInterceptor)
          .failFastIfOffline(failFastIfOffline)
          .listeners(listeners)
    }
  }

  companion object {
    @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("ApolloClient.Builder()"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_0_0)
    @JvmStatic
    fun builder() = Builder()
  }
}
