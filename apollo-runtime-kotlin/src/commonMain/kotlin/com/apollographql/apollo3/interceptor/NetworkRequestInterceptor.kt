package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.ApolloMutationRequest
import com.apollographql.apollo3.ApolloQueryRequest
import com.apollographql.apollo3.ApolloRequest
import com.apollographql.apollo3.ApolloSubscriptionRequest
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.flow.Flow

class NetworkRequestInterceptor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport,
) : ApolloRequestInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return when (request) {
      is ApolloQueryRequest -> networkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache)
      is ApolloMutationRequest -> networkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache)
      is ApolloSubscriptionRequest -> subscriptionNetworkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache)
      // should never happen
    }
  }
}
