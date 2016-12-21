package com.apollostack.android;

public class PostBody {
  private final String query;
  private final String variables;

  public PostBody(String query, String variables) {
    this.query = query;
    this.variables = variables;
  }

  public PostBody(String query) {
    this(query, null);
  }
}
