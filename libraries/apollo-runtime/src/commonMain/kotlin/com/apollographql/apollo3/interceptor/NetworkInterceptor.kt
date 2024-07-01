package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.flow.Flow

internal class NetworkInterceptor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport,
) : ApolloInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return when (request.operation) {
      is Query<*> -> networkTransport.execute(request = request)
      is Mutation<*> -> networkTransport.execute(request = request)
      is Subscription<*> -> subscriptionNetworkTransport.execute(request = request)
      else -> error("")
    }
  }
}
