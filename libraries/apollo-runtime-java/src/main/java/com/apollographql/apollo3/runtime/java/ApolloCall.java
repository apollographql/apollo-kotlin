package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import org.jetbrains.annotations.Nullable;

public interface ApolloCall<D extends Operation.Data> {
  /**
   * Schedules the request to be executed at some point in the future.
   *
   * @param callback Callback which will handle the response or a failure exception.
   */
  ApolloDisposable enqueue(@Nullable ApolloCallback<D> callback);
}
