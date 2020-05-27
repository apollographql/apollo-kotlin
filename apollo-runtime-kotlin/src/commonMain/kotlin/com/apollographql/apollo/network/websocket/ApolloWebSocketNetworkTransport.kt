package com.apollographql.apollo.network.websocket

import com.apollographql.apollo.api.ApolloExperimental
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.interceptor.ApolloRequest
import com.apollographql.apollo.interceptor.ApolloRequestInterceptor
import kotlinx.coroutines.flow.Flow

@ApolloExperimental
class ApolloWebSocketNetworkTransport(
    private val apolloWebSocketFactory: ApolloWebSocketFactory
) : ApolloRequestInterceptor {

  override fun <T> intercept(request: ApolloRequest<T>, interceptorChain: ApolloInterceptorChain): Flow<Response<T>> {
    TODO("Not yet implemented")
  }
}
