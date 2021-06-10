package com.apollographql.apollo3

import com.apollographql.apollo3.api.Adapter
import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.CustomScalarAdapters
import com.apollographql.apollo3.api.ExecutionContext
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.api.http.HttpMethod
import com.apollographql.apollo3.api.http.HttpRequestComposerParams
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo3.interceptor.NetworkInterceptor
import com.apollographql.apollo3.interceptor.RealInterceptorChain
import com.apollographql.apollo3.internal.defaultDispatcher
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.HttpNetworkTransport
import com.apollographql.apollo3.network.ws.WebSocketNetworkTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.single

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
 */
class ApolloClient constructor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport = networkTransport,
    private val customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    private val interceptors: List<ApolloInterceptor> = emptyList(),
    private val executionContext: ExecutionContext = ExecutionContext.Empty,
    private val requestedDispatcher: CoroutineDispatcher? = null,
) {

  private val dispatcher = defaultDispatcher(requestedDispatcher)
  private val clientScope = ClientScope(CoroutineScope(dispatcher))

  suspend fun <D : Query.Data> query(query: Query<D>): ApolloResponse<D> = query(ApolloRequest(query))

  suspend fun <D : Mutation.Data> mutate(mutation: Mutation<D>): ApolloResponse<D> = mutate(ApolloRequest(mutation))

  fun <D : Subscription.Data> subscribe(subscription: Subscription<D>): Flow<ApolloResponse<D>> = subscribe(ApolloRequest(subscription))

  suspend fun <D : Query.Data> query(queryRequest: ApolloRequest<D>): ApolloResponse<D> {
    return queryRequest.execute().single()
  }

  fun <D : Query.Data> queryAsFlow(queryRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return queryRequest.execute()
  }

  suspend fun <D : Mutation.Data> mutate(mutationRequest: ApolloRequest<D>): ApolloResponse<D> {
    return mutationRequest.execute().single()
  }

  fun <D : Mutation.Data> mutateAsFlow(mutationRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return mutationRequest.execute()
  }

  fun <D : Operation.Data> subscribe(subscriptionRequest: ApolloRequest<D>): Flow<ApolloResponse<D>> {
    return subscriptionRequest.execute()
  }

  @OptIn(ExperimentalCoroutinesApi::class)
  private fun <D : Operation.Data> ApolloRequest<D>.execute(): Flow<ApolloResponse<D>> {
    val executionContext = customScalarAdapters + this@ApolloClient.executionContext + this.executionContext

    val request = withExecutionContext(executionContext)
    val interceptors = interceptors + NetworkInterceptor(
        networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
    )

    return flow {
      emit(
          RealInterceptorChain(
              interceptors,
              0,
          )
      )
    }.flatMapLatest { interceptorChain ->
      interceptorChain.proceed(request)
    }.flowOn(dispatcher)
  }

  fun dispose() {
    clientScope.coroutineScope.cancel()
    networkTransport.dispose()
    subscriptionNetworkTransport.dispose()
  }

  fun <T> withCustomScalarAdapter(graphqlName: String, customScalarAdapter: Adapter<T>): ApolloClient {
    return copy(
        customScalarAdapters = CustomScalarAdapters(
            customScalarAdapters.customScalarAdapters + mapOf(graphqlName to customScalarAdapter)
        )
    )
  }

  fun withInterceptor(interceptor: ApolloInterceptor): ApolloClient {
    return copy(
        interceptors = interceptors + interceptor
    )
  }

  fun withExecutionContext(executionContext: ExecutionContext): ApolloClient {
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
      ): ApolloClient {
    return ApolloClient(
        networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
        customScalarAdapters = customScalarAdapters,
        interceptors = interceptors,
        executionContext = executionContext,
        requestedDispatcher = requestedDispatcher,
    )
  }
}

fun ApolloClient(
    serverUrl: String,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    interceptors: List<ApolloInterceptor> = emptyList(),
    executionContext: ExecutionContext = ExecutionContext.Empty,
    requestedDispatcher: CoroutineDispatcher? = null,
) = ApolloClient(
    networkTransport = HttpNetworkTransport(serverUrl = serverUrl),
    subscriptionNetworkTransport = WebSocketNetworkTransport(serverUrl = serverUrl),
    customScalarAdapters = customScalarAdapters,
    interceptors = interceptors,
    executionContext = executionContext,
    requestedDispatcher = requestedDispatcher
)


fun ApolloClient.withAutoPersistedQueries(): ApolloClient {
  return withInterceptor(AutoPersistedQueryInterceptor())
      .withExecutionContext(
          HttpRequestComposerParams(
              HttpMethod.Post,
              sendApqExtensions = true,
              sendDocument = false,
              headers = emptyMap()
          )
      )
}