package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

/** Represents either a successful or failed response received from the GraphQL server. */
public final class Response<T> {
  private final Operation operation;
  private final T data;
  private final List<Error> errors;
  private Set<String> dependentKeys;

  public static <T> Response.Builder<T> builder(@Nonnull final Operation operation) {
    return new Builder<>(operation);
  }

  Response(Builder<T> builder) {
    operation = checkNotNull(builder.operation, "operation == null");
    data = builder.data;
    errors = builder.errors != null ? unmodifiableList(builder.errors) : Collections.<Error>emptyList();
    dependentKeys = builder.dependentKeys != null ? unmodifiableSet(builder.dependentKeys)
        : Collections.<String>emptySet();
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

  public static final class Builder<T> {
    private final Operation operation;
    private T data;
    private List<Error> errors;
    private Set<String> dependentKeys;

    Builder(@Nonnull final Operation operation) {
      this.operation = checkNotNull(operation, "operation == null");
    }

    public Builder<T> data(T data) {
      this.data = data;
      return this;
    }

    public Builder<T> errors(@Nullable List<Error> errors) {
      this.errors = errors;
      return this;
    }

    public Builder<T> dependentKeys(@Nullable Set<String> dependentKeys) {
      this.dependentKeys = dependentKeys;
      return this;
    }

    public Response<T> build() {
      return new Response<>(this);
    }
  }
}
