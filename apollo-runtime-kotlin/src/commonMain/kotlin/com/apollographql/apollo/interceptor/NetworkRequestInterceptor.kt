package com.apollographql.apollo.interceptor

import com.apollographql.apollo.ApolloMutationRequest
import com.apollographql.apollo.ApolloQueryRequest
import com.apollographql.apollo.ApolloRequest
import com.apollographql.apollo.ApolloSubscriptionRequest
import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.dispatcher.ApolloCoroutineDispatcherContext
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@ApolloExperimental
class NetworkRequestInterceptor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport,
    private val coroutineDispatcherContext: ApolloCoroutineDispatcherContext
) : ApolloRequestInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return when (request) {
      is ApolloQueryRequest -> networkTransport.execute(request = request, customScalarAdapters = chain.customScalarAdapters, executionContext = request.executionContext + coroutineDispatcherContext)
      is ApolloMutationRequest -> networkTransport.execute(request = request, customScalarAdapters = chain.customScalarAdapters, executionContext = request.executionContext + coroutineDispatcherContext)
      is ApolloSubscriptionRequest -> subscriptionNetworkTransport.execute(request = request, customScalarAdapters = chain.customScalarAdapters, executionContext = request.executionContext + coroutineDispatcherContext)
      else -> emptyFlow() // should never happen
    }
  }
}
