package com.apollographql.apollo3.interceptor

import com.apollographql.apollo3.api.ApolloRequest
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.Mutation
import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Query
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.network.NetworkTransport
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn

internal class NetworkInterceptor(
    private val networkTransport: NetworkTransport,
    private val subscriptionNetworkTransport: NetworkTransport,
    private val dispatcher: CoroutineDispatcher
) : ApolloInterceptor {

  override fun <D : Operation.Data> intercept(request: ApolloRequest<D>, chain: ApolloInterceptorChain): Flow<ApolloResponse<D>> {
    return when (request.operation) {
      is Query<*> -> networkTransport.execute(request = request)
      is Mutation<*> -> networkTransport.execute(request = request)
      is Subscription<*> -> subscriptionNetworkTransport.execute(request = request)
      else -> error("")
    }.flowOn(dispatcher)
  }
}
