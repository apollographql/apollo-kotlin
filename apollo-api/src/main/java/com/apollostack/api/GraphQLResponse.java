package com.apollostack.api;

import java.util.List;

import javax.annotation.Nullable;

/** Represents either a successful or failed response received from the GraphQL server. */
public class GraphQLResponse<T extends GraphQLOperation.Data> {
  @Nullable private final T data;
  @Nullable private final List<GraphQLError> errors;

  public GraphQLResponse(@Nullable T data, @Nullable List<GraphQLError> errors) {
    this.data = data;
    this.errors = errors;
  }

  public boolean isSuccessful() {
    return errors == null || errors.isEmpty();
  }

  @Nullable public T data() {
    return data;
  }

  @Nullable public List<GraphQLError> errors() {
    return errors;
  }
}
