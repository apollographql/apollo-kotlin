package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.Operation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GraphQlOperationRequest<V extends Operation.Variables> {

  private final String query;

  private final V variables;

  public GraphQlOperationRequest(@Nonnull Operation<V> operation) {
    this.query = operation.queryDocument().replaceAll("\\n", "");
    this.variables = operation.variables();
  }

  public GraphQlOperationRequest(@Nonnull String query, @Nullable V variables) {
    this.query = query.replaceAll("\\n", "");
    this.variables = variables;
  }

  @Nonnull public String query() {
    return query;
  }

  public V variables() {
    return variables;
  }
}
