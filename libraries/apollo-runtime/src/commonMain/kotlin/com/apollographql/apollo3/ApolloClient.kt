package com.apollographql.apollo3

import com.apollographql.apollo3.annotations.ApolloDeprecatedSince
import com.apollographql.apollo3.annotations.ApolloExperimental
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
import com.apollographql.apollo3.exception.ApolloHttpException
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo3.interceptor.DefaultInterceptorChain
import com.apollographql.apollo3.interceptor.NetworkInterceptor
import com.apollographql.apollo3.interceptor.RetryOnErrorInterceptor
import com.apollographql.apollo3.internal.defaultDispatcher
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
import kotlinx.coroutines.flow.onEach
import okio.Closeable
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
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
  override val executionContext: ExecutionContext = builder.executionContext
  override val httpMethod: HttpMethod? = builder.httpMethod
  override val httpHeaders: List<HttpHeader>? = builder.httpHeaders
  override val sendApqExtensions: Boolean? = builder.sendApqExtensions
  override val sendDocument: Boolean? = builder.sendDocument
  override val enableAutoPersistedQueries: Boolean? = builder.enableAutoPersistedQueries
  override val canBeBatched: Boolean? = builder.canBeBatched
  @ApolloExperimental
  override val retryNetworkErrors: Boolean? = builder.retryNetworkErrors

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

  /**
   * Creates a new [ApolloCall] that you can customize and/or execute.
   */
  fun <D : Subscription.Data> subscription(subscription: Subscription<D>): ApolloCall<D> {
    return ApolloCall(this, subscription)
  }

  @Deprecated("Use close() instead", ReplaceWith("close()"), level = DeprecationLevel.ERROR)
  @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v4_0_0)
  fun dispose() {
    close()
  }

  override fun close() {
    concurrencyInfo.coroutineScope.cancel()
    networkTransport.dispose()
    subscriptionNetworkTransport.dispose()
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
  fun <D : Operation.Data> executeAsFlow(
      apolloRequest: ApolloRequest<D>,
  ): Flow<ApolloResponse<D>> {
    return executeAsFlow(apolloRequest, false, false)
  }

  internal fun <D : Operation.Data> executeAsFlow(
      apolloRequest: ApolloRequest<D>,
      ignoreApolloClientHttpHeaders: Boolean,
      throwing: Boolean,
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
        .apply {
          if (apolloRequest.httpMethod != null) {
            httpMethod(apolloRequest.httpMethod)
          }
          val requestHttpHeaders = apolloRequest.httpHeaders.orEmpty()
          httpHeaders(
              if (ignoreApolloClientHttpHeaders) {
                requestHttpHeaders
              } else {
                this@ApolloClient.httpHeaders.orEmpty() + requestHttpHeaders
              }
          )
          if (apolloRequest.sendApqExtensions != null) {
            sendApqExtensions(apolloRequest.sendApqExtensions)
          }
          if (apolloRequest.sendDocument != null) {
            sendDocument(apolloRequest.sendDocument)
          }
          if (apolloRequest.enableAutoPersistedQueries != null) {
            enableAutoPersistedQueries(apolloRequest.enableAutoPersistedQueries)
          }
          val canBeBatched = apolloRequest.canBeBatched ?: this@ApolloClient.canBeBatched
          if (canBeBatched != null) {
            // Because batching is handled at the HTTP level, move the information to HTTP headers
            // canBeBatched(apolloRequest.canBeBatched)
            addHttpHeader(ExecutionOptions.CAN_BE_BATCHED, canBeBatched.toString())
          }
        }
        .build()

    val allInterceptors = buildList{
      addAll(interceptors)
      val retryNetworkErrors = apolloRequest.retryNetworkErrors
          ?: retryNetworkErrors
          ?: (request.operation is Subscription && subscriptionNetworkTransport::class.qualifiedName.orEmpty().startsWith("com.apollographql.apollo3.network.ws.incubating"))
      if (retryNetworkErrors) {
        add(RetryOnErrorInterceptor)
      }
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

  fun newBuilder(): Builder {
    return builder.copy()
  }

  /**
   * A Builder used to create instances of [ApolloClient]
   */
  class Builder : MutableExecutionOptions<Builder> {
    private val _customScalarAdaptersBuilder = CustomScalarAdapters.Builder()
    val customScalarAdapters: CustomScalarAdapters get() = _customScalarAdaptersBuilder.build()

    private val _interceptors: MutableList<ApolloInterceptor> = mutableListOf()
    val interceptors: List<ApolloInterceptor> = _interceptors

    private val _httpInterceptors: MutableList<HttpInterceptor> = mutableListOf()
    val httpInterceptors: List<HttpInterceptor> = _httpInterceptors

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
    @ApolloExperimental
    override var retryNetworkErrors: Boolean? = null
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
    override fun retryNetworkErrors(retryNetworkErrors: Boolean?): Builder = apply {
      this.retryNetworkErrors = retryNetworkErrors
    }

    override fun httpMethod(httpMethod: HttpMethod?): Builder = apply {
      this.httpMethod = httpMethod
    }

    override fun httpHeaders(httpHeaders: List<HttpHeader>?): Builder = apply {
      this.httpHeaders = httpHeaders
    }

    override fun addHttpHeader(name: String, value: String): Builder = apply {
      this.httpHeaders = (this.httpHeaders ?: emptyList()) + HttpHeader(name, value)
    }

    override fun sendApqExtensions(sendApqExtensions: Boolean?): Builder = apply {
      this.sendApqExtensions = sendApqExtensions
    }

    override fun sendDocument(sendDocument: Boolean?): Builder = apply {
      this.sendDocument = sendDocument
    }

    override fun enableAutoPersistedQueries(enableAutoPersistedQueries: Boolean?): Builder = apply {
      this.enableAutoPersistedQueries = enableAutoPersistedQueries
    }

    override fun canBeBatched(canBeBatched: Boolean?): Builder = apply {
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
    fun httpServerUrl(httpServerUrl: String?) = apply {
      this.httpServerUrl = httpServerUrl
    }

    /**
     * The [HttpEngine] to use for HTTP requests
     *
     * See also [networkTransport] for more customization
     */
    fun httpEngine(httpEngine: HttpEngine?) = apply {
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
    fun httpExposeErrorBody(httpExposeErrorBody: Boolean?) = apply {
      this.httpExposeErrorBody = httpExposeErrorBody
    }

    /**
     * Adds [httpInterceptor] to the list of HTTP interceptors
     *
     * See also [networkTransport] for more customization
     */
    fun httpInterceptors(httpInterceptors: List<HttpInterceptor>) = apply {
      _httpInterceptors.clear()
      _httpInterceptors.addAll(httpInterceptors)
    }

    /**
     * Adds [httpInterceptor] to the list of HTTP interceptors
     *
     * See also [networkTransport] for more customization
     */
    fun addHttpInterceptor(httpInterceptor: HttpInterceptor) = apply {
      _httpInterceptors += httpInterceptor
    }

    /**
     * The url of the GraphQL server used for WebSockets
     * Use this function or webSocketServerUrl((suspend () -> String)) but not both.
     *
     * See also [subscriptionNetworkTransport] for more customization
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
     */
    fun webSocketServerUrl(webSocketServerUrl: (suspend () -> String)?) = apply {
      this.webSocketReopenServerUrl = webSocketServerUrl
    }

    /**
     * The timeout after which an inactive WebSocket will be closed
     *
     * See also [subscriptionNetworkTransport] for more customization
     */
    fun webSocketIdleTimeoutMillis(webSocketIdleTimeoutMillis: Long?) = apply {
      this.webSocketIdleTimeoutMillis = webSocketIdleTimeoutMillis
    }

    /**
     * The [WsProtocol.Factory] to use for websockets
     *
     * See also [subscriptionNetworkTransport] for more customization
     */
    fun wsProtocol(wsProtocolFactory: WsProtocol.Factory?) = apply {
      this.wsProtocolFactory = wsProtocolFactory
    }

    /**
     * The [WebSocketEngine] to use for WebSocket requests
     *
     * See also [subscriptionNetworkTransport] for more customization
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
     * See also [subscriptionNetworkTransport] for more customization
     */
    fun webSocketReopenWhen(webSocketReopenWhen: (suspend (Throwable, attempt: Long) -> Boolean)?) = apply {
      this.webSocketReopenWhen = webSocketReopenWhen
    }

    fun networkTransport(networkTransport: NetworkTransport?) = apply {
      this.networkTransport = networkTransport
    }

    fun subscriptionNetworkTransport(subscriptionNetworkTransport: NetworkTransport?) = apply {
      this.subscriptionNetworkTransport = subscriptionNetworkTransport
    }

    fun customScalarAdapters(customScalarAdapters: CustomScalarAdapters) = apply {
      _customScalarAdaptersBuilder.clear()
      _customScalarAdaptersBuilder.addAll(customScalarAdapters)
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
      _customScalarAdaptersBuilder.add(customScalarType, customScalarAdapter)
    }

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
      val builder = Builder()
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
          .retryNetworkErrors(retryNetworkErrors)
      return builder
    }
  }

  companion object {
    @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("ApolloClient.Builder()"), level = DeprecationLevel.ERROR)
    @ApolloDeprecatedSince(ApolloDeprecatedSince.Version.v3_0_0)
    @JvmStatic
    fun builder() = Builder()
  }
}
