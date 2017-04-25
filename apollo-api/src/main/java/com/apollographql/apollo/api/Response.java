package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/** Represents either a successful or failed response received from the GraphQL server. */
public class Response<T> {
  private final Operation operation;
  private final T data;
  private final List<Error> errors;
  private Set<String> dependentKeys;

  public Response(Operation operation) {
    this(operation, null, null, null);
  }

  public Response(Operation operation, T data, List<Error> errors, Set<String> dependentKeys) {
    this.operation = operation;
    this.data = data;
    this.errors = errors != null ? errors : Collections.<Error>emptyList();
    this.dependentKeys = dependentKeys != null ? dependentKeys : Collections.<String>emptySet();
  }

  public Operation operation() {
    return operation;
  }

  @Nullable public T data() {
    return data;
  }

  @Nonnull public List<Error> errors() {
    return errors;
  }

  @Nonnull public Set<String> dependentKeys() {
    return dependentKeys;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }
}
