package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.ApolloMutationRequest
import com.apollographql.apollo3.ApolloQueryRequest
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.ApolloSubscriptionRequest
import com.apollographql.apollo3.api.ApolloExperimental
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.dispatcher.ApolloCoroutineDispatcherContext
import com.apollographql.apollo3.network.NetworkTransport
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
      is ApolloQueryRequest -> networkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache, executionContext = request.executionContext + coroutineDispatcherContext)
      is ApolloMutationRequest -> networkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache, executionContext = request.executionContext + coroutineDispatcherContext)
      is ApolloSubscriptionRequest -> subscriptionNetworkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache, executionContext = request.executionContext + coroutineDispatcherContext)
      else -> emptyFlow() // should never happen
    }
  }
}
