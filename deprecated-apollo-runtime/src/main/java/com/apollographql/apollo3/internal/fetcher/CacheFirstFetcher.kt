package com.apollographql.apollo3.internal.fetcher

import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import java.util.concurrent.Executor

/**
 * Signals the apollo client to first fetch the data from the normalized cache. If it's not present in the normalized
 * cache or if an exception occurs while trying to fetch it from the normalized cache, then the data is instead fetched
 * from the network.
 */
class CacheFirstFetcher : ResponseFetcher {
  override fun provideInterceptor(apolloLogger: ApolloLogger?): ApolloInterceptor {
    return CacheFirstInterceptor()
  }

  private class CacheFirstInterceptor : ApolloInterceptor {
    @Volatile
    var disposed = false
    override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                dispatcher: Executor, callBack: CallBack) {
      val cacheRequest = request.toBuilder().fetchFromCache(true).build()
      chain.proceedAsync(cacheRequest, dispatcher, object : CallBack {
        override fun onResponse(response: InterceptorResponse) {
          callBack.onResponse(response)
        }

        override fun onFailure(e: ApolloException) {
          if (!disposed) {
            val networkRequest = request.toBuilder().fetchFromCache(false).build()
            chain.proceedAsync(networkRequest, dispatcher, callBack)
          }
        }

        override fun onCompleted() {
          callBack.onCompleted()
        }

        override fun onFetch(sourceType: FetchSourceType) {
          callBack.onFetch(sourceType)
        }
      })
    }

    override fun dispose() {
      disposed = true
    }
  }
}