package com.apollographql.apollo3.runtime.java.interceptor;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import org.jetbrains.annotations.NotNull;

/**
 * ApolloInterceptor is responsible for observing and modifying the requests going out and the corresponding responses coming back in.
 * Typical responsibilities include adding or removing headers from the request or response objects, transforming the returned responses
 * from one type to another, etc.
 */
public interface ApolloInterceptor<D extends Operation.Data> {
  /**
   * Intercepts the outgoing request and performs non-blocking operations on the request or the response returned by the next set of
   * interceptors in the chain.
   *
   * @param request outgoing request object.
   * @param chain the ApolloInterceptorChain object containing the next set of interceptors.
   * @param callback the Callback which will handle the interceptor's response or failure exception.
   */
  void intercept(@NotNull ApolloRequest<D> request, @NotNull ApolloInterceptorChain<D> chain, @NotNull ApolloCallback<D> callback);
}
