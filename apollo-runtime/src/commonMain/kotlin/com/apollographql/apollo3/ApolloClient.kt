package com.apollographql.apollo3

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ApolloInternal
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.CustomScalarType
import com.apollographql.apollo3.api.CustomTypeAdapter
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.HasExecutionContext
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.addHttpHeader
import com.apollographql.apollo3.api.http.addHttpHeaders
import com.apollographql.apollo3.api.http.httpMethod
import com.apollographql.apollo3.api.http.sendApqExtensions
import com.apollographql.apollo3.api.http.sendDocument
import com.apollographql.apollo3.api.internal.Version2CustomTypeAdapterToAdapter
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo3.interceptor.DefaultInterceptorChain
import com.apollographql.apollo3.interceptor.NetworkInterceptor
import com.apollographql.apollo3.internal.defaultDispatcher
import com.apollographql.apollo3.mpp.assertMainThreadOnNative
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.HttpEngine
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.ws.WebSocketEngine
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlin.jvm.JvmOverloads
import kotlin.jvm.JvmStatic

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
 */
class ApolloClient @JvmOverloads @Deprecated("Please use ApolloClient.Builder instead. This will be removed in v3.0.0.") constructor(
    val networkTransport: NetworkTransport,
    private val customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    private val subscriptionNetworkTransport: NetworkTransport = networkTransport,
    val interceptors: List<ApolloInterceptor> = emptyList(),
    override val executionContext: ExecutionContext = ExecutionContext.Empty,
    private val requestedDispatcher: CoroutineDispatcher? = null,
) : HasExecutionContext {
  private val concurrencyInfo: ConcurrencyInfo

  init {
    val dispatcher = defaultDispatcher(requestedDispatcher)
    concurrencyInfo = ConcurrencyInfo(
        dispatcher,
        CoroutineScope(dispatcher))

  }

  /**
   * A short-hand constructor
   */
  @Suppress("DEPRECATION")
  @Deprecated("Please use ApolloClient.Builder instead. This will be removed in v3.0.0.", ReplaceWith("ApolloClient.Builder().serverUrl(serverUrl)"))
  constructor(
      serverUrl: String,
  ) : this(
      networkTransport = HttpNetworkTransport(serverUrl = serverUrl),
      subscriptionNetworkTransport = WebSocketNetworkTransport(serverUrl = serverUrl),
  )

  /**
   * Creates a new [ApolloQueryCall] that you can customize and/or execute.
   */
  fun <D : Query.Data> query(query: Query<D>): ApolloQueryCall<D> {
    return ApolloQueryCall(this, query)
  }

  /**
   * Creates a new [ApolloMutationCall] that you can customize and/or execute.
   */
  fun <D : Mutation.Data> mutate(mutation: Mutation<D>): ApolloMutationCall<D> {
    return ApolloMutationCall(this, mutation)
  }

  /**
   * Creates a new [ApolloSubscriptionCall] that you can customize and/or execute.
   */
  fun <D : Subscription.Data> subscribe(subscription: Subscription<D>): ApolloSubscriptionCall<D> {
    return ApolloSubscriptionCall(this, subscription)
  }

  fun dispose() {
    concurrencyInfo.coroutineScope.cancel()
    networkTransport.dispose()
    subscriptionNetworkTransport.dispose()
  }

  @Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
  fun <T> withCustomScalarAdapter(customScalarType: CustomScalarType, customScalarAdapter: Adapter<T>): ApolloClient {
    return newBuilder().customScalarAdapters(
        customScalarAdapters = CustomScalarAdapters.Builder()
            .addAll(customScalarAdapters)
            .add(customScalarType, customScalarAdapter)
            .build()
    ).build()
  }

  @Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
  fun withInterceptor(interceptor: ApolloInterceptor): ApolloClient {
    return newBuilder().addInterceptor(
        interceptor
    ).build()
  }

  @Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
  fun withExecutionContext(executionContext: ExecutionContext): ApolloClient {
    return newBuilder().addExecutionContext(executionContext).build()
  }

  private val networkInterceptor = NetworkInterceptor(
      networkTransport = networkTransport,
      subscriptionNetworkTransport = subscriptionNetworkTransport,
      dispatcher = concurrencyInfo.dispatcher
  )

  /**
   * Low level API to execute the given [apolloRequest] and return a [Flow].
   *
   * Prefer [query], [mutate] or [subscribe] when possible.
   *
   * For simple queries, the returned [Flow] will contain only one element.
   * For more advanced use cases like watchers or subscriptions, it may contain any number of elements and never
   * finish. You can cancel the corresponding coroutine to terminate the [Flow] in this case.
   */
  fun <D : Operation.Data> executeAsFlow(apolloRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    assertMainThreadOnNative()
    val executionContext = concurrencyInfo + customScalarAdapters + executionContext + apolloRequest.executionContext

    val request = apolloRequest.newBuilder().addExecutionContext(executionContext).build()
    // ensureNeverFrozen(request)

    return DefaultInterceptorChain(interceptors + networkInterceptor, 0).proceed(request)
  }

  /**
   * A Builder used to create instances of [ApolloClient]
   */
  class Builder : HasMutableExecutionContext<Builder> {
    private var _networkTransport: NetworkTransport? = null
    private var subscriptionNetworkTransport: NetworkTransport? = null
    private val customScalarAdaptersBuilder = CustomScalarAdapters.Builder()
    private val _interceptors: MutableList<ApolloInterceptor> = mutableListOf()
    val interceptors: List<ApolloInterceptor> = _interceptors
    private var requestedDispatcher: CoroutineDispatcher? = null
    override var executionContext: ExecutionContext = ExecutionContext.Empty
    private var httpServerUrl: String? = null
    private var webSocketServerUrl: String? = null
    private var httpEngine: HttpEngine? = null
    private var webSocketEngine: WebSocketEngine? = null

    /**
     * The url of the GraphQL server used for both HTTP and WebSockets
     */
    fun serverUrl(serverUrl: String) = apply {
      httpServerUrl = serverUrl
      webSocketServerUrl = serverUrl
    }

    /**
     * The url of the GraphQL server used for HTTP
     */
    fun httpServerUrl(httpServerUrl: String) = apply {
      this.httpServerUrl = httpServerUrl
    }

    /**
     * The url of the GraphQL server used for WebSockets
     */
    fun webSocketServerUrl(webSocketServerUrl: String) = apply {
      this.webSocketServerUrl = webSocketServerUrl
    }

    /**
     * The [HttpEngine] to use for HTTP requests
     *
     * For more customization, see also [networkTransport]
     */
    fun httpEngine(httpEngine: HttpEngine) = apply {
      this.httpEngine = httpEngine
    }

    /**
     * The [WebSocketEngine] to use for WebSocket requests
     *
     * For more customization, see also [subscriptionNetworkTransport]
     */
    fun webSocketEngine(webSocketEngine: WebSocketEngine) = apply {
      this.webSocketEngine = webSocketEngine
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
    fun <T> addCustomTypeAdapter(
        customScalarType: CustomScalarType,
        customScalarAdapter: Adapter<T>,
    ) = addCustomScalarAdapter(customScalarType, customScalarAdapter)

    @OptIn(ApolloInternal::class)
    @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("addCustomScalarAdapter"))
    fun <T> addCustomTypeAdapter(
        customScalarType: CustomScalarType,
        @Suppress("DEPRECATION") customTypeAdapter: CustomTypeAdapter<T>,
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

    fun requestedDispatcher(requestedDispatcher: CoroutineDispatcher?) = apply {
      this.requestedDispatcher = requestedDispatcher
    }

    override fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun executionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = executionContext
    }

    fun build(): ApolloClient {
      val networkTransport = if (_networkTransport != null) {
        check(httpServerUrl == null) {
          "ApolloGraphQL: 'httpServerUrl' has no effect if 'networkTransport' is set"
        }
        check(httpEngine == null) {
          "ApolloGraphQL: 'httpEngine' has no effect if 'networkTransport' is set"
        }
        _networkTransport!!
      } else {
        check(httpServerUrl != null) {
          "ApolloGraphQL: 'serverUrl' is required"
        }
        HttpNetworkTransport.Builder()
            .serverUrl(httpServerUrl!!)
            .httpEngine(httpEngine ?: HttpEngine())
            .build()
      }

      val subscriptionNetworkTransport = if (subscriptionNetworkTransport != null) {
        check(webSocketServerUrl == null) {
          "ApolloGraphQL: 'webSocketServerUrl' has no effect if 'subscriptionNetworkTransport' is set"
        }
        check(webSocketEngine == null) {
          "ApolloGraphQL: 'webSocketEngine' has no effect if 'subscriptionNetworkTransport' is set"
        }
        subscriptionNetworkTransport!!
      } else {
        val url = webSocketServerUrl ?: httpServerUrl
        if (url == null) {
          // Fallback to the regular [NetworkTransport]. This is unlikely to work but chances are
          // that the user is not going to use subscription so it's better than failing
          networkTransport
        } else {
          WebSocketNetworkTransport.Builder()
              .serverUrl(webSocketServerUrl!!)
              .webSocketEngine(webSocketEngine ?: WebSocketEngine())
              .build()
        }
      }

      @Suppress("DEPRECATION")
      return ApolloClient(
          networkTransport = networkTransport,
          subscriptionNetworkTransport = subscriptionNetworkTransport,
          customScalarAdapters = customScalarAdaptersBuilder.build(),
          interceptors = _interceptors,
          requestedDispatcher = requestedDispatcher,
          executionContext = executionContext,
      )
    }
  }

  fun newBuilder(): Builder {
    return Builder()
        .networkTransport(networkTransport)
        .subscriptionNetworkTransport(subscriptionNetworkTransport)
        .customScalarAdapters(customScalarAdapters)
        .interceptors(interceptors)
        .requestedDispatcher(requestedDispatcher)
        .executionContext(executionContext)
  }

  companion object {
    @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("ApolloClient.Builder()"))
    @JvmStatic
    fun builder() = Builder()
  }
}


/**
 * Configures the given [ApolloClient] to try auto persisted queries.
 *
 * @param httpMethodForHashedQueries: the [HttpMethod] to use for the initial hashed query that does not send the actual Graphql document.
 * [HttpMethod.Get] allows to use caching when available while [HttpMethod.Post] usually allows bigger document sizes.
 * Only used if [hashByDefault] is true
 * Default: [HttpMethod.Get]
 *
 * @param httpMethodForDocumentQueries: the [HttpMethod] to use for the follow up query that sends the full document if the initial
 * hashed query was not found.
 * Default: [HttpMethod.Post]
 *
 * @param hashByDefault: whether to enable Auto Persisted Queries by default. If true, it will set httpMethodForHashedQueries,
 * sendApqExtensions=true and sendDocument=false.
 * If false it will leave them untouched. You can later use [hashedQuery] to enable them
 */
fun ApolloClient.Builder.autoPersistedQueries(
    httpMethodForHashedQueries: HttpMethod = HttpMethod.Get,
    httpMethodForDocumentQueries: HttpMethod = HttpMethod.Post,
    hashByDefault: Boolean = true,
): ApolloClient.Builder {
  return addInterceptor(AutoPersistedQueryInterceptor(httpMethodForDocumentQueries)).let {
    if (hashByDefault) {
      it.httpMethod(httpMethodForHashedQueries).hashedQuery(true)
    } else {
      it
    }
  }
}

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withAutoPersistedQueries(
    httpMethodForHashedQueries: HttpMethod = HttpMethod.Get,
    httpMethodForDocumentQueries: HttpMethod = HttpMethod.Post,
    hashByDefault: Boolean = true,
): ApolloClient {
  return this.newBuilder().autoPersistedQueries(httpMethodForHashedQueries, httpMethodForDocumentQueries, hashByDefault).build()
}

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpMethod(httpMethod: HttpMethod) = newBuilder().httpMethod(httpMethod).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpHeaders(httpHeaders: List<HttpHeader>) = newBuilder().addHttpHeaders(httpHeaders).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpHeader(httpHeader: HttpHeader) = newBuilder().addHttpHeader(httpHeader).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withHttpHeader(name: String, value: String) = newBuilder().addHttpHeader(name, value).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withSendApqExtensions(sendApqExtensions: Boolean) = newBuilder().sendApqExtensions(sendApqExtensions).build()

@Deprecated("Please use ApolloClient.Builder methods instead. This will be removed in v3.0.0.")
fun ApolloClient.withSendDocument(sendDocument: Boolean) = newBuilder().sendDocument(sendDocument).build()

@Deprecated("Used for backward compatibility with 2.x", ReplaceWith("httpMethod(HttpMethod.Get)", "com.apollographql.apollo3.api.http.httpMethod", "com.apollographql.apollo3.api.http.HttpMethod"))
fun ApolloClient.Builder.useHttpGetMethodForQueries(
    useHttpGetMethodForQueries: Boolean,
) = httpMethod(if (useHttpGetMethodForQueries) HttpMethod.Get else HttpMethod.Post)

@Deprecated("Used for backward compatibility with 2.x. This method throws immediately", ReplaceWith("autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.Get)", "com.apollographql.apollo3.api.http.HttpMethod", "com.apollographql.apollo3.api.http.HttpMethod"))
fun ApolloClient.Builder.useHttpGetMethodForPersistedQueries(
    useHttpGetMethodForQueries: Boolean,
) = apply {
  throw NotImplementedError("useHttpGetMethodForPersistedQueries is now configured at the same time as auto persisted queries. Use autoPersistedQueries(httpMethodForHashedQueries = HttpMethod.GET) instead.")
}
