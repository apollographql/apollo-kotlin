package com.apollographql.android.api.graphql;

import java.util.Collections;
import java.util.List;

/** Represents either a successful or failed response received from the GraphQL server. */
public class Response<T> {
  private final Operation operation;
  private final T data;
  private final List<Error> errors;

  public Response(Operation operation) {
    this(operation, null, null);
  }

  public Response(Operation operation, T data, List<Error> errors) {
    this.operation = operation;
    this.data = data;
    this.errors = errors != null ? errors : Collections.<Error>emptyList();
  }

  public boolean isSuccessful() {
    return errors.isEmpty();
  }

  public Operation operation() {
    return operation;
  }

  public T data() {
    return data;
  }

  public List<Error> errors() {
    return errors;
  }
}
