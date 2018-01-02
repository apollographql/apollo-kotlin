package com.apollographql.apollo.exception;

public class ApolloException extends RuntimeException {

  public ApolloException(String message) {
    super(message);
  }

  public ApolloException(String message, Throwable cause) {
    super(message, cause);
  }
}
