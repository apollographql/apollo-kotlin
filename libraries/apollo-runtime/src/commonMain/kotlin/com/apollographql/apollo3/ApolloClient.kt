package com.apollographql.apollo3

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_0
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_0_1
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_3_1
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_3_3
import com.apollographql.apollo3.annotations.ApolloDeprecatedSince.Version.v3_8_3
import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.CustomScalarType
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.ExecutionOptions
import com.apollographql.apollo3.api.MutableExecutionOptions
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.internal.Version2CustomTypeAdapterToAdapter
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo3.interceptor.DefaultInterceptorChain
import com.apollographql.apollo3.interceptor.NetworkInterceptor
import com.apollographql.apollo3.internal.defaultDispatcher
import com.apollographql.apollo3.internal.failOnNativeIfLegacyMemoryManager
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.BatchingHttpInterceptor
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.HttpInterceptor
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.ws.WebSocketEngine
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import com.apollographql.apollo3.network.ws.WsProtocol
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import okio.Closeable
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
 */
class ApolloClient
private constructor(
    val networkTransport: NetworkTransport,
    val customScalarAdapters: CustomScalarAdapters,
    val subscriptionNetworkTransport: NetworkTransport,
    val interceptors: List<ApolloInterceptor>,
    override val executionContext: ExecutionContext,
    private val dispatcher: CoroutineDispatcher?,
    override val httpMethod: HttpMethod?,
    override val httpHeaders: List<HttpHeader>?,
    override val sendApqExtensions: Boolean?,
    override val sendDocument: Boolean?,
    override val enableAutoPersistedQueries: Boolean?,
    override val canBeBatched: Boolean?,
    private val builder: Builder,
) : ExecutionOptions, Closeable {
  private val concurrencyInfo: ConcurrencyInfo

  init {
    val dispatcher = dispatcher ?: defaultDispatcher
    concurrencyInfo = ConcurrencyInfo(
        dispatcher,
        CoroutineScope(dispatcher))
  }

  /**
   * Creates a new [ApolloCall] that you can customize and/or execute.
   */
  fun <D : Query.Data> query(query: Query<D>): ApolloCall<D> {
    return ApolloCall(this, query)
  }

  /**
   * Creates a new [ApolloCall] that you can customize and/or execute.
   */
  fun <D : Mutation.Data> mutation(mutation: Mutation<D>): ApolloCall<D> {
    return ApolloCall(this, mutation)
  }

  @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("mutation(mutation)"))
  @ApolloDeprecatedSince(v3_0_0)
  fun <D : Mutation.Data> mutate(mutation: Mutation<D>): ApolloCall<D> = mutation(mutation)

  /**
   * Creates a new [ApolloCall] that you can customize and/or execute.
   */
  fun <D : Subscription.Data> subscription(subscription: Subscription<D>): ApolloCall<D> {
    return ApolloCall(this, subscription)
  }

  @Deprecated("Use a query and ignore the result", level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(v3_0_0)
  @Suppress("UNUSED_PARAMETER")
  fun <D : Operation.Data> prefetch(operation: Operation<D>): Nothing {
    throw NotImplementedError()
  }

  @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("subscription(subscription)"))
  @ApolloDeprecatedSince(v3_0_0)
  fun <D : Subscription.Data> subscribe(subscription: Subscription<D>): ApolloCall<D> = subscription(subscription)

  override fun close() {
    concurrencyInfo.coroutineScope.cancel()
    networkTransport.dispose()
    subscriptionNetworkTransport.dispose()
  }

  @Deprecated(
      "Use close() instead or call okio.use { }",
      replaceWith = ReplaceWith("close()"),
  )
  @ApolloDeprecatedSince(v3_3_1)
  fun dispose() {
    close()
  }

  private val networkInterceptor = NetworkInterceptor(
      networkTransport = networkTransport,
      subscriptionNetworkTransport = subscriptionNetworkTransport,
      dispatcher = concurrencyInfo.dispatcher
  )

  /**
   * Low level API to execute the given [apolloRequest] and return a [Flow].
   *
   * Prefer [query], [mutation] or [subscription] when possible.
   *
   * For simple queries, the returned [Flow] will contain only one element.
   * For more advanced use cases like watchers or subscriptions, it may contain any number of elements and never
   * finish. You can cancel the corresponding coroutine to terminate the [Flow] in this case.
   */
  fun <D : Operation.Data> executeAsFlow(apolloRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return executeAsFlow(apolloRequest, true)
  }

  internal fun <D : Operation.Data> executeAsFlow(
      apolloRequest: ApolloRequest<D>,
      ignoreApolloClientHttpHeaders: Boolean,
  ): Flow<ApolloResponse<D>> {
    val executionContext = concurrencyInfo + customScalarAdapters + executionContext + apolloRequest.executionContext

    val request = ApolloRequest.Builder(apolloRequest.operation)
        .addExecutionContext(concurrencyInfo)
        .addExecutionContext(customScalarAdapters)
        .addExecutionContext(executionContext)
        .addExecutionContext(apolloRequest.executionContext)
        .httpMethod(httpMethod)
        .sendApqExtensions(sendApqExtensions)
        .sendDocument(sendDocument)
        .enableAutoPersistedQueries(enableAutoPersistedQueries)
        .httpHeaders(
            when {
              apolloRequest.httpHeaders == null -> this@ApolloClient.httpHeaders
              ignoreApolloClientHttpHeaders -> apolloRequest.httpHeaders
              else -> this@ApolloClient.httpHeaders.orEmpty() + apolloRequest.httpHeaders!!
            }
        )
        .apply {
          if (apolloRequest.httpMethod != null) {
            httpMethod(apolloRequest.httpMethod)
          }
          if (apolloRequest.sendApqExtensions != null) {
            sendApqExtensions(apolloRequest.sendApqExtensions)
          }
          if (apolloRequest.sendDocument != null) {
            sendDocument(apolloRequest.sendDocument)
          }
          if (apolloRequest.enableAutoPersistedQueries != null) {
            enableAutoPersistedQueries(apolloRequest.enableAutoPersistedQueries)
          }
          if (apolloRequest.canBeBatched != null) {
            // Because batching is handled at the HTTP level, move the information to HTTP headers
            // canBeBatched(apolloRequest.canBeBatched)
            addHttpHeader(ExecutionOptions.CAN_BE_BATCHED, apolloRequest.canBeBatched.toString())
          }
        }
        .build()

    return DefaultInterceptorChain(interceptors + networkInterceptor, 0).proceed(request)
  }

  /**
   * A Builder used to create instances of [ApolloClient]
   */
  class Builder : MutableExecutionOptions<Builder> {
    private var _networkTransport: NetworkTransport? = null
    private var subscriptionNetworkTransport: NetworkTransport? = null
    private val customScalarAdaptersBuilder = CustomScalarAdapters.Builder()
    private val _interceptors: MutableList<ApolloInterceptor> = mutableListOf()

    /**
     * The APQ interceptor is special because it needs to be always last.
     * If not, there is the risk that it retries cache queries which make no sense.
     */
    private var apqInterceptor: ApolloInterceptor? = null
    val interceptors: List<ApolloInterceptor> = _interceptors
    private val httpInterceptors: MutableList<HttpInterceptor> = mutableListOf()
    private var dispatcher: CoroutineDispatcher? = null
    override var executionContext: ExecutionContext = ExecutionContext.Empty
    private var httpServerUrl: String? = null
    private var httpEngine: HttpEngine? = null
    private var webSocketServerUrl: String? = null
    private var webSocketIdleTimeoutMillis: Long? = null
    private var wsProtocolFactory: WsProtocol.Factory? = null
    private var httpExposeErrorBody: Boolean? = null
    private var webSocketEngine: WebSocketEngine? = null
    private var webSocketReopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)? = null
    private var webSocketReopenServerUrl: (suspend () -> String)? = null

    init {
      failOnNativeIfLegacyMemoryManager()
    }

    override var httpMethod: HttpMethod? = null
      @Deprecated("Use httpMethod() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun httpMethod(httpMethod: HttpMethod?): Builder = apply {
      @Suppress("DEPRECATION")
      this.httpMethod = httpMethod
    }

    override var httpHeaders: List<HttpHeader>? = null
      @Deprecated("Use httpHeaders() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun httpHeaders(httpHeaders: List<HttpHeader>?): Builder = apply {
      @Suppress("DEPRECATION")
      this.httpHeaders = httpHeaders
    }

    override fun addHttpHeader(name: String, value: String): Builder = apply {
      @Suppress("DEPRECATION")
      this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
    }

    override var sendApqExtensions: Boolean? = null
      @Deprecated("Use sendApqExtensions() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun sendApqExtensions(sendApqExtensions: Boolean?): Builder = apply {
      @Suppress("DEPRECATION")
      this.sendApqExtensions = sendApqExtensions
    }

    override var sendDocument: Boolean? = null
      @Deprecated("Use sendDocument() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun sendDocument(sendDocument: Boolean?): Builder = apply {
      @Suppress("DEPRECATION")
      this.sendDocument = sendDocument
    }

    override var enableAutoPersistedQueries: Boolean? = null
      @Deprecated("Use enableAutoPersistedQueries() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): Builder = apply {
      @Suppress("DEPRECATION")
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
    }

    override var canBeBatched: Boolean? = null
      @Deprecated("Use canBeBatched() instead")
      @ApolloDeprecatedSince(v3_8_3)
      set

    override fun canBeBatched(canBeBatched: Boolean?): Builder = apply {
      @Suppress("DEPRECATION")
      this.canBeBatched = canBeBatched
    }

    /**
     * The url of the GraphQL server used for HTTP
     *
     * This is the same as [httpServerUrl]
     *
     * See also [networkTransport] for more customization
     */
    fun serverUrl(serverUrl: String) = apply {
      httpServerUrl = serverUrl
    }

    /**
     * The url of the GraphQL server used for HTTP
     *
     * See also [networkTransport] for more customization
     */
    fun httpServerUrl(httpServerUrl: String) = apply {
      this.httpServerUrl = httpServerUrl
    }

    /**
     * The [HttpEngine] to use for HTTP requests
     *
     * See also [networkTransport] for more customization
     */
    fun httpEngine(httpEngine: HttpEngine) = apply {
      this.httpEngine = httpEngine
    }

    /**
     * Configures whether to expose the error body in [ApolloHttpException].
     *
     * If you're setting this to `true`, you **must** catch [ApolloHttpException] and close the body explicitly
     * to avoid sockets and other resources leaking.
     *
     * Default: false
     */
    fun httpExposeErrorBody(httpExposeErrorBody: Boolean) = apply {
      this.httpExposeErrorBody = httpExposeErrorBody
    }

    /**
     * Adds [httpInterceptor] to the list of HTTP interceptors
     *
     * See also [networkTransport] for more customization
     */
    fun addHttpInterceptor(httpInterceptor: HttpInterceptor) = apply {
      httpInterceptors += httpInterceptor
    }

    /**
     * The url of the GraphQL server used for WebSockets
     * Use this function or webSocketServerUrl((suspend () -> String)) but not both.
     *
     * See also [subscriptionNetworkTransport] for more customization
     */
    fun webSocketServerUrl(webSocketServerUrl: String) = apply {
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
     */
    fun webSocketServerUrl(webSocketServerUrl: (suspend () -> String)) = apply {
      this.webSocketReopenServerUrl = webSocketServerUrl
    }

    /**
     * The timeout after which an inactive WebSocket will be closed
     *
     * See also [subscriptionNetworkTransport] for more customization
     */
    fun webSocketIdleTimeoutMillis(webSocketIdleTimeoutMillis: Long) = apply {
      this.webSocketIdleTimeoutMillis = webSocketIdleTimeoutMillis
    }

    /**
     * The [WsProtocol.Factory] to use for websockets
     *
     * See also [subscriptionNetworkTransport] for more customization
     */
    fun wsProtocol(wsProtocolFactory: WsProtocol.Factory) = apply {
      this.wsProtocolFactory = wsProtocolFactory
    }

    /**
     * The [WebSocketEngine] to use for WebSocket requests
     *
     * See also [subscriptionNetworkTransport] for more customization
     */
    fun webSocketEngine(webSocketEngine: WebSocketEngine) = apply {
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
     * See also [subscriptionNetworkTransport] for more customization
     */
    fun webSocketReopenWhen(webSocketReopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)) = apply {
      this.webSocketReopenWhen = webSocketReopenWhen
    }

    @Deprecated("Use webSocketReopenWhen(webSocketReopenWhen: (suspend (Throwable, attempt: Long) -> Boolean))")
    @ApolloDeprecatedSince(v3_0_1)
    fun webSocketReconnectWhen(reconnectWhen: ((Throwable) -> Boolean)?) = apply {
      this.webSocketReopenWhen = reconnectWhen?.let {
        val adaptedLambda: suspend (Throwable, Long) -> Boolean = { throwable, _ -> reconnectWhen(throwable) }
        adaptedLambda
      }
    }

    fun networkTransport(networkTransport: NetworkTransport) = apply {
      _networkTransport = networkTransport
    }

    fun subscriptionNetworkTransport(subscriptionNetworkTransport: NetworkTransport) = apply {
      this.subscriptionNetworkTransport = subscriptionNetworkTransport
    }

    fun customScalarAdapters(customScalarAdapters: CustomScalarAdapters) = apply {
      customScalarAdaptersBuilder.clear()
      customScalarAdaptersBuilder.addAll(customScalarAdapters)
    }

    /**
     * Registers the given [customScalarAdapter]
     *
     * @param customScalarType a generated [CustomScalarType]. Every GraphQL custom scalar has a
     * generated class with a static `type` property. For an example, for a `Date` custom scalar,
     * you can use `com.example.Date.type`
     * @param customScalarAdapter the [Adapter] to use for this custom scalar
     */
    fun <T> addCustomScalarAdapter(customScalarType: CustomScalarType, customScalarAdapter: Adapter<T>) = apply {
      customScalarAdaptersBuilder.add(customScalarType, customScalarAdapter)
    }

    @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("addCustomScalarAdapter"))
    @ApolloDeprecatedSince(v3_0_0)
    fun <T> addCustomTypeAdapter(
        customScalarType: CustomScalarType,
        @Suppress("DEPRECATION") customTypeAdapter: com.apollographql.apollo3.api.CustomTypeAdapter<T>,
    ) = addCustomScalarAdapter(customScalarType, Version2CustomTypeAdapterToAdapter(customTypeAdapter))

    fun addInterceptor(interceptor: ApolloInterceptor) = apply {
      _interceptors += interceptor
    }

    fun addInterceptors(interceptors: List<ApolloInterceptor>) = apply {
      this._interceptors += interceptors
    }

    fun interceptors(interceptors: List<ApolloInterceptor>) = apply {
      this._interceptors.clear()
      this._interceptors += interceptors
    }

    @Deprecated("Use dispatcher instead", ReplaceWith("dispatcher(requestedDispatcher)"))
    @ApolloDeprecatedSince(v3_3_3)
    fun requestedDispatcher(requestedDispatcher: CoroutineDispatcher?) = apply {
      dispatcher(requestedDispatcher)
    }

    /**
     * Changes the [CoroutineDispatcher] used for I/O intensive work like reading the
     * network or the cache
     * On the JVM the dispatcher is [kotlinx.coroutines.Dispatchers.IO] by default.
     * On native this function has no effect. Network request use the default NSURLConnection
     * threads and the cache uses a background dispatch queue.
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
     * @param httpMethodForHashedQueries: the [HttpMethod] to use for the initial hashed query that does not send the actual Graphql document.
     * [HttpMethod.Get] allows to use caching when available while [HttpMethod.Post] usually allows bigger document sizes. Mutations are
     * always sent using [HttpMethod.Post] regardless of this setting.
     * Default: [HttpMethod.Get]
     *
     * @param httpMethodForDocumentQueries: the [HttpMethod] to use for the follow-up query that sends the full document if the initial
     * hashed query was not found. Mutations are always sent using [HttpMethod.Post] regardless of this setting.
     * Default: [HttpMethod.Post]
     *
     * @param enableByDefault: whether to enable Auto Persisted Queries by default. You can later use
     * [ApolloCall.enableAutoPersistedQueries] on to enable/disable them on individual calls.
     */
    @JvmOverloads
    fun autoPersistedQueries(
        httpMethodForHashedQueries: HttpMethod = HttpMethod.Get,
        httpMethodForDocumentQueries: HttpMethod = HttpMethod.Post,
        enableByDefault: Boolean = true,
    ) = apply {
      apqInterceptor = AutoPersistedQueryInterceptor(
          httpMethodForHashedQueries,
          httpMethodForDocumentQueries
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
      addHttpInterceptor(BatchingHttpInterceptor(batchIntervalMillis, maxBatchSize))
      canBeBatched(enableByDefault)
    }

    @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("httpMethod(HttpMethod.Get)", "com.apollographql.apollo3.api.http.httpMethod", "com.apollographql.apollo3.api.http.HttpMethod"))
    @ApolloDeprecatedSince(v3_0_0)
    fun useHttpGetMethodForQueries(
        useHttpGetMethodForQueries: Boolean,
    ) = httpMethod(if (useHttpGetMethodForQueries) HttpMethod.Get else HttpMethod.Post)

    @Deprecated("Used for backward compatibility with 2.x. This method throws immediately", ReplaceWith("autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.Get)", "com.apollographql.apollo3.api.http.HttpMethod", "com.apollographql.apollo3.api.http.HttpMethod"))
    @ApolloDeprecatedSince(v3_0_0)
    @Suppress("UNUSED_PARAMETER")
    fun useHttpGetMethodForPersistedQueries(
        useHttpGetMethodForQueries: Boolean,
    ) = apply {
      throw NotImplementedError("useHttpGetMethodForPersistedQueries is now configured at the same time as auto persisted queries. Use autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.GET) instead.")
    }

    /**
     * Creates an [ApolloClient] from this [Builder]
     */
    fun build(): ApolloClient {
      val networkTransport = if (_networkTransport != null) {
        check(httpServerUrl == null) {
          "Apollo: 'httpServerUrl' has no effect if 'networkTransport' is set"
        }
        check(httpEngine == null) {
          "Apollo: 'httpEngine' has no effect if 'networkTransport' is set"
        }
        check(httpInterceptors.isEmpty()) {
          "Apollo: 'addHttpInterceptor' has no effect if 'networkTransport' is set"
        }
        check(httpExposeErrorBody == null) {
          "Apollo: 'httpExposeErrorBody' has no effect if 'networkTransport' is set"
        }
        _networkTransport!!
      } else {
        check(httpServerUrl != null) {
          "Apollo: 'serverUrl' is required"
        }
        HttpNetworkTransport.Builder()
            .serverUrl(httpServerUrl!!)
            .apply {
              if (httpEngine != null) {
                httpEngine(httpEngine!!)
              }
              if (httpExposeErrorBody != null) {
                exposeErrorBody(httpExposeErrorBody!!)
              }
            }
            .interceptors(httpInterceptors)
            .build()
      }

      val subscriptionNetworkTransport = if (subscriptionNetworkTransport != null) {
        check(webSocketServerUrl == null) {
          "Apollo: 'webSocketServerUrl' has no effect if 'subscriptionNetworkTransport' is set"
        }
        check(webSocketEngine == null) {
          "Apollo: 'webSocketEngine' has no effect if 'subscriptionNetworkTransport' is set"
        }
        check(webSocketIdleTimeoutMillis == null) {
          "Apollo: 'webSocketIdleTimeoutMillis' has no effect if 'subscriptionNetworkTransport' is set"
        }
        check(wsProtocolFactory == null) {
          "Apollo: 'wsProtocolFactory' has no effect if 'subscriptionNetworkTransport' is set"
        }
        check(webSocketReopenWhen == null) {
          "Apollo: 'webSocketReopenWhen' has no effect if 'subscriptionNetworkTransport' is set"
        }
        check(webSocketReopenServerUrl == null) {
          "Apollo: 'webSocketReopenServerUrl' has no effect if 'subscriptionNetworkTransport' is set"
        }
        subscriptionNetworkTransport!!
      } else {
        val url = webSocketServerUrl ?: httpServerUrl
        if (url == null) {
          // Fallback to the regular [NetworkTransport]. This is unlikely to work but chances are
          // that the user is not going to use subscription, so it's better than failing
          networkTransport
        } else {
          WebSocketNetworkTransport.Builder()
              .serverUrl(url)
              .apply {
                if (webSocketEngine != null) {
                  webSocketEngine(webSocketEngine!!)
                }
                if (webSocketIdleTimeoutMillis != null) {
                  idleTimeoutMillis(webSocketIdleTimeoutMillis!!)
                }
                if (wsProtocolFactory != null) {
                  protocol(wsProtocolFactory!!)
                }
                if (webSocketReopenWhen != null) {
                  reopenWhen(webSocketReopenWhen)
                }
                if (webSocketReopenServerUrl != null) {
                  serverUrl(webSocketReopenServerUrl)
                }
              }
              .build()
        }
      }

      return ApolloClient(
          networkTransport = networkTransport,
          subscriptionNetworkTransport = subscriptionNetworkTransport,
          customScalarAdapters = customScalarAdaptersBuilder.build(),
          interceptors = _interceptors + listOfNotNull(apqInterceptor),
          dispatcher = dispatcher,
          executionContext = executionContext,
          httpMethod = httpMethod,
          httpHeaders = httpHeaders,
          sendApqExtensions = sendApqExtensions,
          sendDocument = sendDocument,
          enableAutoPersistedQueries = enableAutoPersistedQueries,
          canBeBatched = canBeBatched,

          // Keep a reference to the Builder so we can keep track of `httpEngine` and other properties that 
          // are important to rebuild `networkTransport` (and potentially others)
          builder = this,
      )
    }

    fun copy(): Builder {
      val builder = Builder()
          .customScalarAdapters(customScalarAdaptersBuilder.build())
          .interceptors(interceptors)
          .dispatcher(dispatcher)
          .executionContext(executionContext)
          .httpMethod(httpMethod)
          .httpHeaders(httpHeaders)
          .sendApqExtensions(sendApqExtensions)
          .sendDocument(sendDocument)
          .enableAutoPersistedQueries(enableAutoPersistedQueries)
          .canBeBatched(canBeBatched)
      _networkTransport?.let { builder.networkTransport(it) }
      httpServerUrl?.let { builder.httpServerUrl(it) }
      httpEngine?.let { builder.httpEngine(it) }
      httpExposeErrorBody?.let { builder.httpExposeErrorBody(it) }
      for (httpInterceptor in httpInterceptors) {
        builder.addHttpInterceptor(httpInterceptor)
      }
      subscriptionNetworkTransport?.let { builder.subscriptionNetworkTransport(it) }
      webSocketServerUrl?.let { builder.webSocketServerUrl(it) }
      webSocketReopenServerUrl?.let { builder.webSocketServerUrl(it) }
      webSocketEngine?.let { builder.webSocketEngine(it) }
      webSocketReopenWhen?.let { builder.webSocketReopenWhen(it) }
      webSocketIdleTimeoutMillis?.let { builder.webSocketIdleTimeoutMillis(it) }
      wsProtocolFactory?.let { builder.wsProtocol(it) }
      return builder
    }
  }

  fun newBuilder(): Builder {
    return builder.copy()
  }

  companion object {
    @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("ApolloClient.Builder()"))
    @ApolloDeprecatedSince(v3_0_0)
    @JvmStatic
    fun builder() = Builder()
  }
}
