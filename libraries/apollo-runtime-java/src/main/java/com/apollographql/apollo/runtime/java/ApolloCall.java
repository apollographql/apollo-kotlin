package com.apollographql.apollo.runtime.java;

import com.apollographql.apollo.api.MutableExecutionOptions;
import com.apollographql.apollo.api.Operation;
import org.jetbrains.annotations.NotNull;

public interface ApolloCall<D extends Operation.Data> extends MutableExecutionOptions<ApolloCall<D>> {
  /**
   * Schedules the request to be executed at some point in the future.
   *
   * @param callback Callback which will handle the response or a failure exception.
   */
  ApolloDisposable enqueue(@NotNull ApolloCallback<D> callback);
}
