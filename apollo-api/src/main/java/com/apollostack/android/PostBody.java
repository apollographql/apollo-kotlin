package com.apollostack.android;

import com.apollostack.api.GraphQLOperation;
import com.apollostack.api.GraphQLQuery;

import javax.annotation.Nullable;

public class PostBody<T extends GraphQLOperation.Variables> {
  private final String query;
  @Nullable private final T variables;

  public PostBody(GraphQLQuery<T> query) {
    this.query = query.queryDocument();
    this.variables = query.variables();
  }

  public String query() {
    return query;
  }

  @Nullable public T variables() {
    return variables;
  }
}
