package com.apollographql.apollo

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.CustomScalarAdapter
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.CustomScalar
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.dispatcher.ApolloCoroutineDispatcherContext
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo.interceptor.NetworkRequestInterceptor
import com.apollographql.apollo.internal.RealApolloCall
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.network.http.ApolloHttpNetworkTransport
import kotlinx.coroutines.Dispatchers

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
 */
@ApolloExperimental
class ApolloClient private constructor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport,
    private val customScalarAdapters: CustomScalarAdapters,
    private val interceptors: List<ApolloRequestInterceptor>,
    private val executionContext: ExecutionContext
) {
  private val coroutineDispatcherContext = executionContext[ApolloCoroutineDispatcherContext]
      ?: ApolloCoroutineDispatcherContext(Dispatchers.Default)

  fun <D : Operation.Data, V : Operation.Variables> mutate(mutation: Mutation<D, V>): ApolloMutationCall<D> {
    return mutation.prepareCall()
  }

  fun <D : Operation.Data, V : Operation.Variables> query(query: Query<D, V>): ApolloQueryCall<D> {
    return query.prepareCall()
  }

  fun <D : Operation.Data, V : Operation.Variables> subscribe(query: Subscription<D, V>): ApolloQueryCall<D> {
    return query.prepareCall()
  }

  private fun <D : Operation.Data, V : Operation.Variables> Operation<D, V>.prepareCall(): RealApolloCall<D> {
    return RealApolloCall(
        operation = this,
        scalarTypeAdapters = customScalarAdapters,
        interceptors = interceptors + NetworkRequestInterceptor(
            networkTransport = networkTransport,
            subscriptionNetworkTransport = subscriptionNetworkTransport
        ),
        executionContext = executionContext + coroutineDispatcherContext
    )
  }

  fun newBuilder(): Builder {
    return Builder()
        .networkTransport(networkTransport)
        .subscriptionNetworkTransport(subscriptionNetworkTransport)
        .scalarTypeAdapters(customScalarAdapters.customScalarAdapters)
        .interceptors(interceptors)
        .executionContext(executionContext)
  }

  class Builder {
    private var scalarTypeAdapters = emptyMap<CustomScalar, CustomScalarAdapter<*>>()

    private var networkTransport: NetworkTransport? = null
    private var subscriptionNetworkTransport: NetworkTransport? = null
    private var interceptors: List<ApolloRequestInterceptor> = emptyList()
    private var executionContext: ExecutionContext = ExecutionContext.Empty

    fun serverUrl(serverUrl: String) = apply {
      networkTransport(ApolloHttpNetworkTransport(serverUrl = serverUrl, headers = emptyMap()))
    }

    fun addScalarTypeAdapter(customScalar: CustomScalar, customScalarAdapter: CustomScalarAdapter<*>) = apply {
      this.scalarTypeAdapters = this.scalarTypeAdapters + (customScalar to customScalarAdapter)
    }

    fun networkTransport(networkTransport: NetworkTransport) = apply {
      check(this.networkTransport == null) {
        "ApolloGraphQL: networkTransport is already set. If you're using serverUrl(), you shouldn't call networkTransport() manually"
      }
      this.networkTransport = networkTransport
    }

    fun subscriptionNetworkTransport(subscriptionNetworkTransport: NetworkTransport) = apply {
      check(this.subscriptionNetworkTransport == null) {
        "ApolloGraphQL: subscriptionNetworkTransport is already set."
      }
      this.subscriptionNetworkTransport = subscriptionNetworkTransport
    }

    fun addInterceptor(interceptor: ApolloRequestInterceptor, executionContext: ExecutionContext = ExecutionContext.Empty) = apply {
      interceptors = interceptors + interceptor
      this.executionContext = this.executionContext + executionContext
    }

    fun build(): ApolloClient {
      val transport = networkTransport
      check(transport != null) {
        "ApolloGraphQL: no networkTransport, either call networkTransport() or serverUrl()"
      }
      val subscriptionTransport = subscriptionNetworkTransport ?: transport

      return ApolloClient(
          networkTransport = transport,
          subscriptionNetworkTransport = subscriptionTransport,
          customScalarAdapters = CustomScalarAdapters(scalarTypeAdapters),
          interceptors = interceptors,
          executionContext = executionContext
      )
    }

    /**
     * internal because only used from tests
     */
    internal fun interceptors(interceptors: List<ApolloRequestInterceptor>) = apply {
      check(this.interceptors.isEmpty()) {
        "ApolloGraphQL: interceptors is already set"
      }
      this.interceptors = interceptors
    }

    /**
     * Convenience overload of [interceptors] with variadic parameters
     */
    internal fun interceptors(vararg interceptors: ApolloRequestInterceptor) = apply {
      interceptors(interceptors.toList())
    }

    /**
     * internal because only used from tests
     */
    internal fun executionContext(executionContext: ExecutionContext) = apply {
      check(this.executionContext == ExecutionContext.Empty) {
        "ApolloGraphQL: executionContext is already set."
      }
      this.executionContext = executionContext
    }

    /**
     * internal because only used from tests
     */
    fun scalarTypeAdapters(customCustomScalarAdapters: Map<CustomScalar, CustomScalarAdapter<*>>) = apply {
      this.scalarTypeAdapters = customCustomScalarAdapters
    }
  }
}
