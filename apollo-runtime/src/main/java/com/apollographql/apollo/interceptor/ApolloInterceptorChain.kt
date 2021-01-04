package com.apollographql.apollo.interceptor

import com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorRequest
import com.apollographql.apollo.interceptor.ApolloInterceptor.CallBack
import java.util.concurrent.Executor

/**
 * ApolloInterceptorChain is responsible for building chain of [ApolloInterceptor] .
 */
interface ApolloInterceptorChain {
  /**
   * Passes the control over to the next [ApolloInterceptor] in the responsibility chain and immediately exits as
   * this is a non blocking call. In order to receive the results back, pass in a callback which will handle the
   * received response or error.
   *
   * @param request    outgoing request object.
   * @param dispatcher the [Executor] which dispatches the calls asynchronously.
   * @param callBack   the callback which will handle the response or a failure exception.
   */
  fun proceedAsync(request: InterceptorRequest, dispatcher: Executor,
                   callBack: CallBack)

  /**
   * Disposes of the resources which are no longer required.
   */
  fun dispose()
}