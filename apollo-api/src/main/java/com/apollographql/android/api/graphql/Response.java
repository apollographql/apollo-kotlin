package com.apollographql.android.api.graphql;

import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Represents either a successful or failed response received from the GraphQL server. */
public class Response<T extends Operation.Data> {
  @Nonnull private final Operation operation;
  @Nullable private final T data;
  @Nullable private final List<Error> errors;

  public Response(@Nonnull Operation operation, @Nullable T data, @Nullable List<Error> errors) {
    this.operation = operation;
    this.data = data;
    this.errors = errors;
  }

  public boolean isSuccessful() {
    return errors == null || errors.isEmpty();
  }

  @Nonnull public Operation operation() {
    return operation;
  }

  @Nullable public T data() {
    return data;
  }

  @Nullable public List<Error> errors() {
    return errors;
  }
}
