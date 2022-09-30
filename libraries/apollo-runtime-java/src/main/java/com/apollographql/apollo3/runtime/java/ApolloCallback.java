package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.exception.ApolloException;
import org.jetbrains.annotations.NotNull;

public interface ApolloCallback<D extends Operation.Data> {
  /**
   * Gets called when GraphQL response is received and parsed successfully. D
   *
   * @param response the GraphQL response
   */
  public abstract void onResponse(@NotNull ApolloResponse<D> response);

  /**
   * Gets called when an unexpected exception occurs while creating the request or processing the response.
   */
  public abstract void onFailure(@NotNull ApolloException e);
}