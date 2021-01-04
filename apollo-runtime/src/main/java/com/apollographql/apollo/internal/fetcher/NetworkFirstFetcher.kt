package com.apollographql.apollo.internal.fetcher

import com.apollographql.apollo.api.internal.ApolloLogger
import com.apollographql.apollo.exception.ApolloException
import com.apollographql.apollo.fetcher.ResponseFetcher
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import java.util.concurrent.Executor

/**
 * Signals the apollo client to first fetch the data from the network. If network request fails, then the data is
 * fetched from the normalized cache. If the data is not present in the normalized cache, then the exception which led
 * to the network request failure is rethrown.
 */
class NetworkFirstFetcher : ResponseFetcher {
  override fun provideInterceptor(logger: ApolloLogger?): ApolloInterceptor? {
    return NetworkFirstInterceptor(logger)
  }

  private class NetworkFirstInterceptor internal constructor(val logger: ApolloLogger?) : ApolloInterceptor {
    @Volatile
    var disposed = false
    override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                dispatcher: Executor, callBack: CallBack) {
      val networkRequest = request.toBuilder().fetchFromCache(false).build()
      chain.proceedAsync(networkRequest, dispatcher, object : CallBack {
        override fun onResponse(response: InterceptorResponse) {
          callBack.onResponse(response)
        }

        override fun onFailure(networkException: ApolloException) {
          logger!!.d(networkException, "Failed to fetch network response for operation %s, trying to return cached one",
              request.operation.name().name())
          if (!disposed) {
            val cacheRequest = request.toBuilder().fetchFromCache(true).build()
            chain.proceedAsync(cacheRequest, dispatcher, object : CallBack {
              override fun onResponse(response: InterceptorResponse) {
                callBack.onResponse(response)
              }

              override fun onFetch(sourceType: FetchSourceType?) {
                callBack.onFetch(sourceType)
              }

              override fun onFailure(cacheException: ApolloException) {
                callBack.onFailure(networkException)
              }

              override fun onCompleted() {
                callBack.onCompleted()
              }
            })
          }
        }

        override fun onCompleted() {
          callBack.onCompleted()
        }

        override fun onFetch(sourceType: FetchSourceType?) {
          callBack.onFetch(sourceType)
        }
      })
    }

    override fun dispose() {
      disposed = true
    }
  }
}