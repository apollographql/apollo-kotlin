package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Operation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class GraphQlOperationRequest<V extends Operation.Variables> {

  private final String query;

  private final V variables;

  public GraphQlOperationRequest(@Nonnull Operation<V> operation) {
    this.query = operation.queryDocument().replaceAll("\\n", "");
    this.variables = operation.variables();
  }

  public GraphQlOperationRequest(@Nonnull String queryDocument, @Nullable V variables) {
    this.query = queryDocument.replaceAll("\\n", "");
    this.variables = variables;
  }

  @Nonnull public String query() {
    return query;
  }

  public V variables() {
    return variables;
  }
}
