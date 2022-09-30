package com.apollographql.apollo3.runtime.java.interceptor;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import org.jetbrains.annotations.NotNull;

/**
 * ApolloInterceptorChain is responsible for building chain of {@link ApolloInterceptor} .
 */
public interface ApolloInterceptorChain<D extends Operation.Data> {
  boolean isDisposed();

  /**
   * Passes the control over to the next {@link ApolloInterceptor} in the responsibility chain and immediately exits as this is a
   * non-blocking call. In order to receive the results back, pass in a callback which will handle the received response or error.
   *
   * @param request outgoing request object.
   * @param callBack the callback which will handle the response or a failure exception.
   */
  void proceed(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callBack);
}
