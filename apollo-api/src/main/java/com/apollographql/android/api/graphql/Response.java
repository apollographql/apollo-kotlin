package com.apollographql.android.api.graphql;

import java.util.List;

import javax.annotation.Nullable;

/** Represents either a successful or failed response received from the GraphQL server. */
public class Response<T extends com.apollographql.android.api.graphql.Operation.Data> {
  @Nullable private final T data;
  @Nullable private final List<com.apollographql.android.api.graphql.Error> errors;

  public Response(@Nullable T data, @Nullable List<com.apollographql.android.api.graphql.Error> errors) {
    this.data = data;
    this.errors = errors;
  }

  public boolean isSuccessful() {
    return errors == null || errors.isEmpty();
  }

  @Nullable public T data() {
    return data;
  }

  @Nullable public List<com.apollographql.android.api.graphql.Error> errors() {
    return errors;
  }
}
