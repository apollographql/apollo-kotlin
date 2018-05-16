package com.apollographql.apollo.api;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

/** Represents either a successful or failed response received from the GraphQL server. */
public final class Response<T> {
  private final Operation operation;
  private final T data;
  private final List<Error> errors;
  private Set<String> dependentKeys;
  private final boolean fromCache;

  public static <T> Response.Builder<T> builder(@NotNull final Operation operation) {
    return new Builder<>(operation);
  }

  Response(Builder<T> builder) {
    operation = checkNotNull(builder.operation, "operation == null");
    data = builder.data;
    errors = builder.errors != null ? unmodifiableList(builder.errors) : Collections.<Error>emptyList();
    dependentKeys = builder.dependentKeys != null ? unmodifiableSet(builder.dependentKeys)
        : Collections.<String>emptySet();
    fromCache = builder.fromCache;
  }

  public Operation operation() {
    return operation;
  }

  @Nullable public T data() {
    return data;
  }

  @NotNull public List<Error> errors() {
    return errors;
  }

  @NotNull public Set<String> dependentKeys() {
    return dependentKeys;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean fromCache() {
    return fromCache;
  }

  public Builder<T> toBuilder() {
    return new Builder<T>(operation)
        .data(data)
        .errors(errors)
        .dependentKeys(dependentKeys)
        .fromCache(fromCache);
  }

  public static final class Builder<T> {
    final Operation operation;
    T data;
    List<Error> errors;
    Set<String> dependentKeys;
    boolean fromCache;

    Builder(@NotNull final Operation operation) {
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

    public Builder<T> fromCache(boolean fromCache) {
      this.fromCache = fromCache;
      return this;
    }

    public Response<T> build() {
      return new Response<>(this);
    }
  }
}
