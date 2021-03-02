package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.ApolloRequest

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.flow.Flow

class NetworkRequestInterceptor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport,
) : ApolloRequestInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return when (request.operation) {
      is Query<*> -> networkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache)
      is Mutation<*> -> networkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache)
      is Subscription<*> -> subscriptionNetworkTransport.execute(request = request, responseAdapterCache = chain.responseAdapterCache)
      else -> error("")
    }
  }
}
