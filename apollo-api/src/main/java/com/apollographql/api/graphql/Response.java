package com.apollographql.api.graphql;

import java.util.List;

import javax.annotation.Nullable;

/** Represents either a successful or failed response received from the GraphQL server. */
public class Response<T extends Operation.Data> {
  @Nullable private final T data;
  @Nullable private final List<Error> errors;

  public Response(@Nullable T data, @Nullable List<Error> errors) {
    this.data = data;
    this.errors = errors;
  }

  public boolean isSuccessful() {
    return errors == null || errors.isEmpty();
  }

  @Nullable public T data() {
    return data;
  }

  @Nullable public List<Error> errors() {
    return errors;
  }
}
