package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.internal.batch.BatchPoller
import com.apollographql.apollo.internal.batch.QueryToBatch
import java.util.concurrent.Executor

/**
 * Interceptor used when batching is enabled.
 * Responsible for "pausing" the interceptor chain until the {@link BatchPoller} triggers a batch HTTP call and
 * resumes the interceptor chain.
 */
class ApolloBatchingInterceptor(
    private val batcher: BatchPoller
) : ApolloInterceptor {

  private var queryToBatch : QueryToBatch? = null

  override fun interceptAsync(
      request: ApolloInterceptor.InterceptorRequest,
      chain: ApolloInterceptorChain,
      dispatcher: Executor,
      callBack: ApolloInterceptor.CallBack
  ) {
    queryToBatch = QueryToBatch(request, callBack).also {
      batcher.enqueue(it)
    }
  }

  override fun dispose() {
    queryToBatch?.let { batcher.removeFromQueue(it) }
  }
}