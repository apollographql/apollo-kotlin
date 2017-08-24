package com.apollographql.apollo.interceptor;

import java.util.concurrent.Executor;

import javax.annotation.Nonnull;

/**
 * ApolloInterceptorChain is responsible for building chain of {@link ApolloInterceptor} .
 */
public interface ApolloInterceptorChain {
  /**
   * Passes the control over to the next {@link ApolloInterceptor} in the responsibility chain and immediately exits as
   * this is a non blocking call. In order to receive the results back, pass in a callback which will handle the
   * received response or error.
   *
   * @param request    outgoing request object.
   * @param dispatcher the {@link Executor} which dispatches the calls asynchronously.
   * @param callBack   the callback which will handle the response or a failure exception.
   */
  void proceedAsync(@Nonnull ApolloInterceptor.InterceptorRequest request, @Nonnull Executor dispatcher,
      @Nonnull ApolloInterceptor.CallBack callBack);

  /**
   * Disposes of the resources which are no longer required.
   */
  void dispose();
}
