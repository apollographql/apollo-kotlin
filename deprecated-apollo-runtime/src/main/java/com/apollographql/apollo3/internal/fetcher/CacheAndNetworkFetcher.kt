package com.apollographql.apollo3.internal.fetcher

import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.api.internal.Optional.Companion.absent
import com.apollographql.apollo3.api.internal.Optional.Companion.of
import com.apollographql.apollo3.api.exception.ApolloException
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import java.util.concurrent.Executor

/**
 * Signal the apollo client to fetch the data from both the network and the cache. If cached data is not present, only
 * network data will be returned. If cached data is available, but network experiences an error, cached data is
 * returned. If cache data is not available, and network data is not available, the error of the network request will be
 * propagated. If both network and cache are available, both will be returned. Cache data is guaranteed to be returned
 * first.
 */
class CacheAndNetworkFetcher : ResponseFetcher {
  override fun provideInterceptor(apolloLogger: ApolloLogger?): ApolloInterceptor {
    return CacheAndNetworkInterceptor()
  }

  private class CacheAndNetworkInterceptor : ApolloInterceptor {
    private var cacheResponse = absent<InterceptorResponse>()
    private var networkResponse = absent<InterceptorResponse>()
    private var cacheException = absent<ApolloException>()
    private var networkException = absent<ApolloException>()
    private var dispatchedCacheResult = false
    private var originalCallback: CallBack? = null

    @Volatile
    private var disposed = false
    override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                dispatcher: Executor, callBack: CallBack) {
      if (disposed) return
      originalCallback = callBack
      val cacheRequest = request.toBuilder().fetchFromCache(true).build()
      chain.proceedAsync(cacheRequest, dispatcher, object : CallBack {
        override fun onResponse(response: InterceptorResponse) {
          handleCacheResponse(response)
        }

        override fun onFailure(e: ApolloException) {
          handleCacheError(e)
        }

        override fun onCompleted() {}
        override fun onFetch(sourceType: FetchSourceType) {
          callBack.onFetch(sourceType)
        }
      })
      val networkRequest = request.toBuilder().fetchFromCache(false).build()
      chain.proceedAsync(networkRequest, dispatcher, object : CallBack {
        override fun onResponse(response: InterceptorResponse) {
          handleNetworkResponse(response)
        }

        override fun onFailure(e: ApolloException) {
          handleNetworkError(e)
        }

        override fun onCompleted() {}
        override fun onFetch(sourceType: FetchSourceType) {
          callBack.onFetch(sourceType)
        }
      })
    }

    override fun dispose() {
      disposed = true
    }

    @Synchronized
    fun handleNetworkResponse(response: InterceptorResponse) {
      networkResponse = of(response)
      dispatch()
    }

    @Synchronized
    fun handleNetworkError(exception: ApolloException) {
      networkException = of(exception)
      dispatch()
    }

    @Synchronized
    fun handleCacheResponse(response: InterceptorResponse) {
      cacheResponse = of(response)
      dispatch()
    }

    @Synchronized
    fun handleCacheError(exception: ApolloException) {
      cacheException = of(exception)
      dispatch()
    }

    @Synchronized
    private fun dispatch() {
      if (disposed) {
        return
      }
      if (!dispatchedCacheResult) {
        if (cacheResponse.isPresent) {
          originalCallback!!.onResponse(cacheResponse.get())
          dispatchedCacheResult = true
        } else if (cacheException.isPresent) {
          dispatchedCacheResult = true
        }
      }
      // Only send the network result after the cache result has been dispatched
      if (dispatchedCacheResult) {
        if (networkResponse.isPresent) {
          originalCallback!!.onResponse(networkResponse.get())
          originalCallback!!.onCompleted()
        } else if (networkException.isPresent) {
          originalCallback!!.onFailure(networkException.get())
        }
      }
    }
  }
}