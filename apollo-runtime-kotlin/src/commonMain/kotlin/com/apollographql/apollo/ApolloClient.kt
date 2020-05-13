package com.apollographql.apollo

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.ExecutionContext
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.ScalarTypeAdapters
import com.apollographql.apollo.interceptor.NetworkRequestInterceptor
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import com.apollographql.apollo.internal.RealApolloCall
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ApolloExperimental
@ExperimentalCoroutinesApi
class ApolloClient constructor(
    private val networkTransport: NetworkTransport,
    private val scalarTypeAdapters: ScalarTypeAdapters = ScalarTypeAdapters.DEFAULT,
    private val interceptors: List<ApolloRequestInterceptor> = emptyList(),
    private val executionContext: ExecutionContext = ExecutionContext.Empty
) {

  fun <D : Operation.Data, T, V : Operation.Variables> mutate(mutation: Mutation<D, T, V>): ApolloMutationCall<T> {
    return RealApolloCall(
        operation = mutation,
        scalarTypeAdapters = scalarTypeAdapters,
        interceptors = interceptors + NetworkRequestInterceptor(networkTransport),
        executionContext = executionContext
    )
  }

  fun <D : Operation.Data, T, V : Operation.Variables> query(query: Query<D, T, V>): ApolloQueryCall<T> {
    return RealApolloCall(
        operation = query,
        scalarTypeAdapters = scalarTypeAdapters,
        interceptors = interceptors + NetworkRequestInterceptor(networkTransport),
        executionContext = executionContext
    )
  }
}
