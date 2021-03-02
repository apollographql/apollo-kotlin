package com.apollographql.apollo3.internal.fetcher

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.api.internal.ApolloLogger
import com.apollographql.apollo3.exception.ApolloException
import com.apollographql.apollo3.fetcher.ResponseFetcher
import com.apollographql.apollo3.interceptor.ApolloInterceptor
import com.apollographql.apollo3.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo3.interceptor.ApolloInterceptor.FetchSourceType
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo3.interceptor.ApolloInterceptor.InterceptorResponse
import com.apollographql.apollo3.interceptor.ApolloInterceptorChain
import com.apollographql.apollo3.withCacheInfo
import com.benasher44.uuid.uuid4
import java.util.concurrent.Executor

/**
 * Signals the apollo client to **only** fetch the data from the normalized cache. If it's not present in the
 * normalized cache or if an exception occurs while trying to fetch it from the normalized cache, an empty [ ] is sent back with the [com.apollographql.apollo3.api.Operation] info
 * wrapped inside.
 */
class CacheOnlyFetcher : ResponseFetcher {
  override fun provideInterceptor(logger: ApolloLogger?): ApolloInterceptor {
    return CacheOnlyInterceptor()
  }

  private class CacheOnlyInterceptor : ApolloInterceptor {
    override fun interceptAsync(request: InterceptorRequest, chain: ApolloInterceptorChain,
                                dispatcher: Executor, callBack: CallBack) {
      val cacheRequest = request.toBuilder().fetchFromCache(true).build()
      chain.proceedAsync(cacheRequest, dispatcher, object : CallBack {
        override fun onResponse(response: InterceptorResponse) {
          callBack.onResponse(response)
        }

        override fun onFailure(e: ApolloException) {
          // Cache only returns null instead of throwing when the cache is empty
          callBack.onResponse(cacheMissResponse(request.operation))
          callBack.onCompleted()
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
      //no-op
    }

    fun cacheMissResponse(operation: Operation<*>?): InterceptorResponse {
      return InterceptorResponse(
          null,
          ApolloResponse(
              requestUuid = uuid4(),
              operation = operation!!,
              data = null,
          ).withCacheInfo(true)
      )
    }
  }
}