package com.apollographql.apollo3

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.CustomScalarType
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.ExecutionParameters
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.withHttpMethod
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo3.interceptor.NetworkInterceptor
import com.apollographql.apollo3.interceptor.DefaultInterceptorChain
import com.apollographql.apollo3.internal.defaultDispatcher
import com.apollographql.apollo3.mpp.ensureNeverFrozen
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
class ApolloClient @JvmOverloads constructor(
    val networkTransport: NetworkTransport,
    private val customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    private val subscriptionNetworkTransport: NetworkTransport = networkTransport,
    val interceptors: List<ApolloInterceptor> = emptyList(),
    override val executionContext: ExecutionContext = ExecutionContext.Empty,
    private val requestedDispatcher: CoroutineDispatcher? = null,
    private val flowDecorators: List<FlowDecorator> = emptyList(),
) : ExecutionParameters<ApolloClient> {

  private val dispatcher = defaultDispatcher(requestedDispatcher)
  private val clientScope = ClientScope(CoroutineScope(dispatcher))

  /**
   * A short-hand constructor
   */
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
  suspend fun <D : Query.Data> query(query: Query<D>): ApolloResponse<D> = query(ApolloRequest(query))

  /**
   * Executes the given mutation and returns a response or throws on transport errors
   * use [mutation] ([ApolloRequest]) to customize the request
   */
  suspend fun <D : Mutation.Data> mutate(mutation: Mutation<D>): ApolloResponse<D> = mutate(ApolloRequest(mutation))

  /**
   * Subscribes to the given subscription. The subscription is cancelled when the coroutine collecting the flow is canceled
   */
  fun <D : Subscription.Data> subscribe(subscription: Subscription<D>): Flow<ApolloResponse<D>> = subscribe(ApolloRequest(subscription))

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

  /**
   * Registers the given [customScalarAdapter]
   *
   * @param customScalarType a generated [CustomScalarType] from the [Types] generated object
   * @param customScalarAdapter the [Adapter] to use for this custom scalar
   */
  fun <T> withCustomScalarAdapter(customScalarType: CustomScalarType, customScalarAdapter: Adapter<T>): ApolloClient {
    return copy(
        customScalarAdapters = CustomScalarAdapters(
            customScalarAdapters.customScalarAdapters + mapOf(customScalarType.name to customScalarAdapter)
        )
    )
  }

  fun withInterceptor(interceptor: ApolloInterceptor): ApolloClient {
    return copy(
        interceptors = interceptors + interceptor
    )
  }

  fun withFlowDecorator(flowDecorator: FlowDecorator): ApolloClient {
    return copy(
        flowDecorators = flowDecorators + flowDecorator
    )
  }

  override fun withExecutionContext(executionContext: ExecutionContext): ApolloClient {
    return copy(
        executionContext = this.executionContext + executionContext
    )
  }

  fun copy(
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
    val executionContext = clientScope + customScalarAdapters + this@ApolloClient.executionContext + this.executionContext

    val request = withExecutionContext(executionContext)
    ensureNeverFrozen(request)
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
 * If false it will leave them untouched. You can later use [withHashedQuery] to enable them
 */
fun ApolloClient.withAutoPersistedQueries(
    httpMethodForHashedQueries: HttpMethod = HttpMethod.Get,
    httpMethodForDocumentQueries: HttpMethod = HttpMethod.Post,
    hashByDefault: Boolean = true,
): ApolloClient {
  return withInterceptor(AutoPersistedQueryInterceptor(httpMethodForDocumentQueries)).let {
    if (hashByDefault) {
      it.withHttpMethod(httpMethodForHashedQueries).withHashedQuery(true)
    } else {
      it
    }
  }
}