package com.apollographql.apollo.exception;

public final class ApolloParseException extends ApolloException {

  public ApolloParseException(String message) {
    super(message);
  }

  public ApolloParseException(String message, Throwable cause) {
    super(message, cause);
  }
}
