package com.apollographql.apollo

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.dispatcher.ApolloCoroutineDispatcherContext
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo.interceptor.NetworkRequestInterceptor
import com.apollographql.apollo.internal.RealApolloCall
import com.apollographql.apollo.network.NetworkTransport
import com.apollographql.apollo.network.http.ApolloHttpNetworkTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

/**
 * The main entry point for the Apollo runtime. An [ApolloClient] is responsible for executing queries, mutations and subscriptions
 *
 * Use the auto-generated [ApolloClient.Builder] to create one
 */
@ApolloExperimental
class ApolloClient private constructor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport,
    private val scalarTypeAdapters: ScalarTypeAdapters,
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
        scalarTypeAdapters = scalarTypeAdapters,
        interceptors = interceptors + NetworkRequestInterceptor(
            networkTransport = networkTransport,
            subscriptionNetworkTransport = subscriptionNetworkTransport
        ),
        executionContext = executionContext + coroutineDispatcherContext
    )
  }

  fun newBuilder(): Builder {
    return DefaultBuilder(scalarTypeAdapters)
        .networkTransport(networkTransport)
        .subscriptionNetworkTransport(subscriptionNetworkTransport)
        .interceptors(interceptors)
        .executionContext(executionContext)
  }

  class DefaultBuilder(override val scalarTypeAdapters: ScalarTypeAdapters = ScalarTypeAdapters.DEFAULT) : Builder()

  abstract class Builder {
    internal abstract val scalarTypeAdapters: ScalarTypeAdapters

    private var networkTransport: NetworkTransport? = null
    private var subscriptionNetworkTransport: NetworkTransport? = null
    private var interceptors: List<ApolloRequestInterceptor> = emptyList()
    private var executionContext: ExecutionContext = ExecutionContext.Empty

    fun serverUrl(serverUrl: String): Builder {
      networkTransport(ApolloHttpNetworkTransport(serverUrl = serverUrl, headers = emptyMap()))
      return this
    }

    fun networkTransport(networkTransport: NetworkTransport): Builder {
      check(this.networkTransport == null) {
        "ApolloGraphQL: networkTransport is already set. If you're using serverUrl(), you shouldn't call networkTransport() manually"
      }
      this.networkTransport = networkTransport
      return this
    }

    fun subscriptionNetworkTransport(subscriptionNetworkTransport: NetworkTransport): Builder {
      check(this.subscriptionNetworkTransport == null) {
        "ApolloGraphQL: subscriptionNetworkTransport is already set."
      }
      this.subscriptionNetworkTransport = subscriptionNetworkTransport
      return this
    }

    fun addInterceptor(interceptor: ApolloRequestInterceptor, executionContext: ExecutionContext = ExecutionContext.Empty): Builder {
      interceptors = interceptors + interceptor
      this.executionContext = this.executionContext + executionContext
      return this
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
          scalarTypeAdapters = scalarTypeAdapters,
          interceptors = interceptors,
          executionContext = executionContext
      )
    }

    /**
     * internal because only used from tests
     */
    internal fun interceptors(interceptors: List<ApolloRequestInterceptor>): Builder {
      check(this.interceptors.isEmpty()) {
        "ApolloGraphQL: interceptors is already set"
      }
      this.interceptors = interceptors
      return this
    }

    /**
     * Convenience overload of [interceptors] with variadic parameters
     */
    internal fun interceptors(vararg interceptors: ApolloRequestInterceptor): Builder {
      return interceptors(interceptors.toList())
    }

    /**
     * internal because only used from tests
     */
    internal fun executionContext(executionContext: ExecutionContext): Builder {
      check(this.executionContext == ExecutionContext.Empty) {
        "ApolloGraphQL: executionContext is already set."
      }
      this.executionContext = executionContext
      return this
    }
  }

  /**
   * Do not remove, it's used to add extension functions
   */
  companion object
}
