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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ApolloExperimental
@ExperimentalCoroutinesApi
class ApolloClient(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport = networkTransport,
    private val scalarTypeAdapters: ScalarTypeAdapters = ScalarTypeAdapters.DEFAULT,
    private val interceptors: List<ApolloRequestInterceptor> = emptyList(),
    private val executionContext: ExecutionContext = ExecutionContext.Empty
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
}
