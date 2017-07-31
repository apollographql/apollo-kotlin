package com.apollographql.apollo.interceptor;

import com.apollographql.apollo.exception.ApolloException;

import java.util.concurrent.ExecutorService;

import javax.annotation.Nonnull;

/**
 * ApolloInterceptorChain is responsible for building chain of {@link ApolloInterceptor} .
 */
public interface ApolloInterceptorChain {

  /**
   * Passes the control over to the next {@link ApolloInterceptor} in the responsibility chain and blocks until the
   * {@link com.apollographql.apollo.interceptor.ApolloInterceptor.InterceptorResponse} is received or is an error.
   *
   * @param request outgoing request object.
   * @return The successful or failed response.
   * @throws ApolloException If an unexpected error occurs.
   */
  @Nonnull ApolloInterceptor.InterceptorResponse proceed(@Nonnull ApolloInterceptor.InterceptorRequest request)
      throws ApolloException;

  /**
   * Passes the control over to the next {@link ApolloInterceptor} in the responsibility chain and immediately exits as
   * this is a non blocking call. In order to receive the results back, pass in a callback which will handle the
   * received response or error.
   *
   * @param request    outgoing request object.
   * @param dispatcher the {@link ExecutorService} which dispatches the calls asynchronously.
   * @param callBack   the callback which will handle the response or a failure exception.
   */
  void proceedAsync(@Nonnull ApolloInterceptor.InterceptorRequest request, @Nonnull ExecutorService dispatcher,
      @Nonnull ApolloInterceptor.CallBack callBack);

  /**
   * Disposes of the resources which are no longer required.
   */
  void dispose();

}
