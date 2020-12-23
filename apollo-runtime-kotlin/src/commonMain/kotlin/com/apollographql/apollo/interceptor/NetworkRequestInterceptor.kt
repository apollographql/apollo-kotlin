package com.apollographql.apollo.interceptor

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Mutation
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Query
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.network.NetworkTransport
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

@ApolloExperimental
class NetworkRequestInterceptor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport
) : ApolloRequestInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return when (request.operation) {
      is Query -> networkTransport.execute(request = request, executionContext = request.executionContext)
      is Mutation -> networkTransport.execute(request = request, executionContext = request.executionContext)
      is Subscription -> subscriptionNetworkTransport.execute(request = request, executionContext = request.executionContext)
      else -> emptyFlow() // should never happen
    }
  }
}
