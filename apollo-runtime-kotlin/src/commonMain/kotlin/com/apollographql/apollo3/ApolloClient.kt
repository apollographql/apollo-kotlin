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
import com.apollographql.apollo3.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo3.interceptor.AutoPersistedQueryInterceptor
import com.apollographql.apollo3.interceptor.NetworkRequestInterceptor
import com.apollographql.apollo3.interceptor.RealInterceptorChain
import com.apollographql.apollo3.internal.maybeFlowOn
import com.apollographql.apollo3.network.NetworkTransport
import com.apollographql.apollo3.network.http.ApolloHttpNetworkTransport
import com.apollographql.apollo3.network.ws.ApolloWebSocketNetworkTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
 */
class ApolloClient constructor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport,
    private val customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    private val interceptors: List<ApolloRequestInterceptor> = emptyList(),
    private val executionContext: ExecutionContext = ExecutionContext.Empty,
    private val jvmDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
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

  private fun <D : Operation.Data> ApolloRequest<D>.execute(): Flow<ApolloResponse<D>> {
    val executionContext = this@ApolloClient.executionContext + customScalarAdapters + this.executionContext

    val request = withExecutionContext(executionContext)
    val interceptors = interceptors + NetworkRequestInterceptor(
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
    }.maybeFlowOn(jvmDispatcher)
  }

  fun <T> withCustomScalarAdapter(graphqlName: String, customScalarAdapter: Adapter<T>): ApolloClient {
    return ApolloClient(
        networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
        interceptors = interceptors,
        executionContext = executionContext,
        jvmDispatcher = jvmDispatcher,
        customScalarAdapters = CustomScalarAdapters(
            customScalarAdapters.customScalarAdapters + mapOf(graphqlName to customScalarAdapter)
        )
    )
  }

  fun withInterceptor(interceptor: ApolloRequestInterceptor): ApolloClient {
    return ApolloClient(
        networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
        customScalarAdapters = customScalarAdapters,
        executionContext = executionContext,
        jvmDispatcher = jvmDispatcher,
        interceptors = interceptors + interceptor
    )
  }

  fun withExecutionContext(executionContext: ExecutionContext): ApolloClient {
    return ApolloClient(
        networkTransport = networkTransport,
        subscriptionNetworkTransport = subscriptionNetworkTransport,
        customScalarAdapters = customScalarAdapters,
        interceptors = interceptors,
        jvmDispatcher = jvmDispatcher,
        executionContext = this.executionContext + executionContext
    )
  }
}

fun ApolloClient(
    serverUrl: String,
    customScalarAdapters: CustomScalarAdapters = CustomScalarAdapters.Empty,
    interceptors: List<ApolloRequestInterceptor> = emptyList(),
    executionContext: ExecutionContext = ExecutionContext.Empty,
    jvmDispatcher: CoroutineDispatcher = Dispatchers.Default,
) = ApolloClient(
    networkTransport = ApolloHttpNetworkTransport(serverUrl = serverUrl),
    subscriptionNetworkTransport = ApolloWebSocketNetworkTransport(serverUrl = serverUrl),
    customScalarAdapters = customScalarAdapters,
    interceptors = interceptors,
    executionContext = executionContext,
    jvmDispatcher = jvmDispatcher
)


fun ApolloClient.withAutoPersistedQueries(): ApolloClient {
  return withInterceptor(AutoPersistedQueryInterceptor())
      .withExecutionContext(HttpRequestComposerParams(HttpMethod.Post, true, false, emptyMap()))
}