package com.apollographql.apollo.exception;

public final class ApolloCanceledException extends ApolloException {

  public ApolloCanceledException(String message) {
    super(message);
  }

  public ApolloCanceledException(String message, Throwable cause) {
    super(message, cause);
  }
}
