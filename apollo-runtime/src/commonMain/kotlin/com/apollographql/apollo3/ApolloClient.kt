package com.apollographql.apollo3

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.CustomScalarType
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.HasExecutionContext
import com.apollographql.apollo3.api.HasMutableExecutionContext
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.http.HttpHeader
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.httpHeader
import com.apollographql.apollo3.api.http.httpHeaders
import com.apollographql.apollo3.api.http.httpMethod
import com.apollographql.apollo3.api.http.sendApqExtensions
import com.apollographql.apollo3.api.http.sendDocument
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo3.interceptor.DefaultInterceptorChain
import com.apollographql.apollo3.interceptor.NetworkInterceptor
import com.apollographql.apollo3.internal.defaultDispatcher
import com.apollographql.apollo3.mpp.assertMainThreadOnNative
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.single
import kotlin.jvm.JvmOverloads

typealias FlowDecorator = (Flow<ApolloResponse<*>>) -> Flow<ApolloResponse<*>>

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
 */
class ApolloClient @JvmOverloads @Deprecated("Please use ApolloClient.Builder instead.  This will be removed in v3.0.0.") constructor(
    private val networkTransport: NetworkTransport,
    private val customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    private val subscriptionNetworkTransport: NetworkTransport = networkTransport,
    val interceptors: List<ApolloInterceptor> = emptyList(),
    override val executionContext: ExecutionContext = ExecutionContext.Empty,
    private val requestedDispatcher: CoroutineDispatcher? = null,
    private val flowDecorators: List<FlowDecorator> = emptyList(),
) : HasExecutionContext {

  private val dispatcher = defaultDispatcher(requestedDispatcher)
  private val clientScope = ClientScope(CoroutineScope(dispatcher))

  /**
   * A short-hand constructor
   */
  @Deprecated("Please use ApolloClient.Builder instead.  This will be removed in v3.0.0.")
  constructor(
      serverUrl: String,
  ) : this(
      networkTransport = HttpNetworkTransport(serverUrl = serverUrl),
      subscriptionNetworkTransport = WebSocketNetworkTransport(serverUrl = serverUrl),
  )

  /**
   * Executes the given query and returns a response or throws on transport errors
   * use [query] ([ApolloRequest]) to customize the request
   */
  suspend fun <D : Query.Data> query(query: Query<D>): ApolloResponse<D> = query(ApolloRequest.Builder(query).build())

  /**
   * Executes the given mutation and returns a response or throws on transport errors
   * use [mutation] ([ApolloRequest]) to customize the request
   */
  suspend fun <D : Mutation.Data> mutate(mutation: Mutation<D>): ApolloResponse<D> = mutate(ApolloRequest.Builder(mutation).build())

  /**
   * Subscribes to the given subscription. The subscription is cancelled when the coroutine collecting the flow is canceled
   */
  fun <D : Subscription.Data> subscribe(subscription: Subscription<D>): Flow<ApolloResponse<D>> = subscribe(ApolloRequest.Builder(subscription).build())

  /**
   * Executes the given queryRequest and returns a response or throws on transport errors
   */
  suspend fun <D : Query.Data> query(queryRequest: ApolloRequest<D>): ApolloResponse<D> {
    return queryRequest.execute().single()
  }

  /**
   * Executes the given queryRequest and returns a Flow of responses.
   *
   * It is used by [watch] when multiple responses are expected in response to a single query
   */
  fun <D : Query.Data> queryAsFlow(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return queryRequest.execute()
  }

  /**
   * Executes the given mutationRequest and returns a response or throws on transport errors
   */
  suspend fun <D : Mutation.Data> mutate(mutationRequest: ApolloRequest<D>): ApolloResponse<D> {
    return mutationRequest.execute().single()
  }

  /**
   * Executes the given mutationRequest and returns a Flow of response or throws on transport errors
   */
  fun <D : Mutation.Data> mutateAsFlow(mutationRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return mutationRequest.execute()
  }

  /**
   * Executes the given subscriptionRequest and returns a response or throws on transport errors
   */
  fun <D : Operation.Data> subscribe(subscriptionRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return subscriptionRequest.execute()
  }

  fun dispose() {
    clientScope.coroutineScope.cancel()
    networkTransport.dispose()
    subscriptionNetworkTransport.dispose()
  }

  @Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
  fun <T> withCustomScalarAdapter(customScalarType: CustomScalarType, customScalarAdapter: Adapter<T>): ApolloClient {
    return copy(
        customScalarAdapters = CustomScalarAdapters(
            customScalarAdapters.customScalarAdapters + mapOf(customScalarType.name to customScalarAdapter)
        )
    )
  }

  @Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
  fun withInterceptor(interceptor: ApolloInterceptor): ApolloClient {
    return copy(
        interceptors = interceptors + interceptor
    )
  }

  @Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
  fun withFlowDecorator(flowDecorator: FlowDecorator): ApolloClient {
    return copy(
        flowDecorators = flowDecorators + flowDecorator
    )
  }

  @Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
  fun withExecutionContext(executionContext: ExecutionContext): ApolloClient {
    return copy(
        executionContext = this.executionContext + executionContext
    )
  }

  private fun copy(
      networkTransport: NetworkTransport = this.networkTransport,
      subscriptionNetworkTransport: NetworkTransport = this.subscriptionNetworkTransport,
      customScalarAdapters: CustomScalarAdapters = this.customScalarAdapters,
      interceptors: List<ApolloInterceptor> = this.interceptors,
      executionContext: ExecutionContext = this.executionContext,
      requestedDispatcher: CoroutineDispatcher? = this.requestedDispatcher,
      flowDecorators: List<FlowDecorator> = this.flowDecorators,
  ): ApolloClient {
    return ApolloClient(
        networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
        customScalarAdapters = customScalarAdapters,
        interceptors = interceptors,
        executionContext = executionContext,
        requestedDispatcher = requestedDispatcher,
        flowDecorators = flowDecorators
    )
  }

  @OptIn(ExperimentalCoroutinesApi::class, kotlinx.coroutines.FlowPreview::class)
  private fun <D : Operation.Data> ApolloRequest<D>.execute(): Flow<ApolloResponse<D>> {
    assertMainThreadOnNative()
    val executionContext = clientScope + customScalarAdapters + this@ApolloClient.executionContext + this.executionContext

    val request = newBuilder().addExecutionContext(executionContext).build()
    // ensureNeverFrozen(request)
    val interceptors = interceptors + NetworkInterceptor(
        networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
    )

    val interceptorChain = DefaultInterceptorChain(interceptors, 0)
    return flowDecorators.fold(interceptorChain.proceed(request).flowOn(dispatcher)) { flow, decorator ->
      @Suppress("UNCHECKED_CAST")
      decorator.invoke(flow) as Flow<ApolloResponse<D>>
    }
  }

  class Builder internal constructor(
      private var _networkTransport: NetworkTransport?,
      private var subscriptionNetworkTransport: NetworkTransport?,
      private var customScalarAdapters: MutableMap<String, Adapter<*>>,
      private val interceptors: MutableList<ApolloInterceptor>,
      private val flowDecorators: MutableList<FlowDecorator>,
      private var requestedDispatcher: CoroutineDispatcher?,
      override var executionContext: ExecutionContext,
  ) : HasMutableExecutionContext<Builder> {

    val networkTransport: NetworkTransport?
      get() = _networkTransport

    constructor() : this(
        _networkTransport = null,
        subscriptionNetworkTransport = null,
        customScalarAdapters = mutableMapOf(),
        interceptors = mutableListOf(),
        flowDecorators = mutableListOf(),
        requestedDispatcher = null,
        executionContext = ExecutionContext.Empty,
    )

    fun serverUrl(serverUrl: String) = apply {
      _networkTransport = HttpNetworkTransport(serverUrl = serverUrl)
      subscriptionNetworkTransport = WebSocketNetworkTransport(serverUrl = serverUrl)
    }

    fun networkTransport(networkTransport: NetworkTransport) = apply {
      _networkTransport = networkTransport
    }

    fun subscriptionNetworkTransport(subscriptionNetworkTransport: NetworkTransport) = apply {
      this.subscriptionNetworkTransport = subscriptionNetworkTransport
    }

    /**
     * Registers the given [customScalarAdapter]
     *
     * @param customScalarType a generated [CustomScalarType] from the [Types] generated object
     * @param customScalarAdapter the [Adapter] to use for this custom scalar
     */
    fun <T> addCustomScalarAdapter(customScalarType: CustomScalarType, customScalarAdapter: Adapter<T>) = apply {
      customScalarAdapters[customScalarType.name] = customScalarAdapter
    }

    fun addInterceptor(interceptor: ApolloInterceptor) = apply {
      interceptors += interceptor
    }

    fun addFlowDecorator(flowDecorator: FlowDecorator) = apply {
      flowDecorators += flowDecorators + flowDecorator
    }

    fun requestedDispatcher(requestedDispatcher: CoroutineDispatcher) = apply {
      this.requestedDispatcher = requestedDispatcher
    }

    override fun addExecutionContext(executionContext: ExecutionContext) = apply {
      this.executionContext = this.executionContext + executionContext
    }

    fun build(): ApolloClient {
      check(_networkTransport != null) {
        "NetworkTransport not set, please call either serverUrl() or networkTransport()"
      }
      return ApolloClient(
          networkTransport = _networkTransport!!,
          subscriptionNetworkTransport = subscriptionNetworkTransport ?: _networkTransport!!,
          customScalarAdapters = CustomScalarAdapters(customScalarAdapters),
          interceptors = interceptors,
          flowDecorators = flowDecorators,
          requestedDispatcher = requestedDispatcher,
          executionContext = executionContext,
      )
    }
  }

  fun newBuilder(): Builder {
    return Builder(
        _networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
        customScalarAdapters = customScalarAdapters.customScalarAdapters.toMutableMap(),
        interceptors = interceptors.toMutableList(),
        flowDecorators = flowDecorators.toMutableList(),
        requestedDispatcher = requestedDispatcher,
        executionContext = executionContext,
    )
  }

  companion object {
    @Deprecated("Used for backward compatibility with 2.x", ReplaceWith("ApolloClient.Builder()"))
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

@Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
fun ApolloClient.withAutoPersistedQueries(
    httpMethodForHashedQueries: HttpMethod = HttpMethod.Get,
    httpMethodForDocumentQueries: HttpMethod = HttpMethod.Post,
    hashByDefault: Boolean = true,
): ApolloClient {
  return this.newBuilder().autoPersistedQueries(httpMethodForHashedQueries, httpMethodForDocumentQueries, hashByDefault).build()
}

@Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
fun ApolloClient.withHttpMethod(httpMethod: HttpMethod) = newBuilder().httpMethod(httpMethod).build()

@Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
fun ApolloClient.withHttpHeaders(httpHeaders: List<HttpHeader>) = newBuilder().httpHeaders(httpHeaders).build()

@Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
fun ApolloClient.withHttpHeader(httpHeader: HttpHeader) = newBuilder().httpHeader(httpHeader).build()

@Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
fun ApolloClient.withHttpHeader(name: String, value: String) = newBuilder().httpHeader(name, value).build()

@Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
fun ApolloClient.withSendApqExtensions(sendApqExtensions: Boolean) = newBuilder().sendApqExtensions(sendApqExtensions).build()

@Deprecated("Please use ApolloClient.Builder methods instead.  This will be removed in v3.0.0.")
fun ApolloClient.withSendDocument(sendDocument: Boolean) = newBuilder().sendDocument(sendDocument).build()
