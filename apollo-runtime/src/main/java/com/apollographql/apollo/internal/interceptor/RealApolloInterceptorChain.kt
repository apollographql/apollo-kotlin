package com.apollographql.apollo.internal.interceptor

import com.apollographql.apollo.api.internal.Utils.__checkNotNull
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack
import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import java.util.ArrayList
import java.util.concurrent.Executor

/**
 * RealApolloInterceptorChain is responsible for building the entire interceptor chain. Based on the task at hand, the
 * chain may contain interceptors which fetch responses from the normalized cache, make network calls to fetch data if
 * needed or parse the http responses to inflate models.
 */
class RealApolloInterceptorChain private constructor(interceptors: List<ApolloInterceptor>, interceptorIndex: Int) : ApolloInterceptorChain {
  private val interceptors: List<ApolloInterceptor>
  private val interceptorIndex: Int

  constructor(interceptors: List<ApolloInterceptor>) : this(interceptors, 0) {}

  override fun proceedAsync(request: InterceptorRequest,
                            dispatcher: Executor, callBack: CallBack) {
    check(interceptorIndex < interceptors.size)
    interceptors[interceptorIndex].interceptAsync(request, RealApolloInterceptorChain(interceptors,
        interceptorIndex + 1), dispatcher, callBack)
  }

  override fun dispose() {
    for (interceptor in interceptors) {
      interceptor.dispose()
    }
  }

  init {
    require(interceptorIndex <= interceptors.size)
    this.interceptors = ArrayList(__checkNotNull(interceptors, "interceptors == null"))
    this.interceptorIndex = interceptorIndex
  }
}