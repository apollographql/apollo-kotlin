package com.apollographql.apollo3.internal.fetcher

import com.apollographql.apollo3.api.ApolloLogger
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import java.util.concurrent.Executor

/**
 * Signals the apollo client to **only** fetch the GraphQL data from the network. If network request fails, an
 * exception is thrown.
 */
class NetworkOnlyFetcher : ResponseFetcher {
  override fun provideInterceptor(apolloLogger: ApolloLogger?): ApolloInterceptor {
    return NetworkOnlyInterceptor()
  }

  private class NetworkOnlyInterceptor : ApolloInterceptor {
    override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                dispatcher: Executor, callBack: CallBack) {
      val networkRequest = request.toBuilder().fetchFromCache(false).build()
      chain.proceedAsync(networkRequest, dispatcher, callBack)
    }

    override fun dispose() {}
  }
}