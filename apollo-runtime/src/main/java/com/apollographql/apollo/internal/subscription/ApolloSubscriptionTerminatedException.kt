package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.exception.ApolloException;

public class ApolloSubscriptionTerminatedException extends ApolloException {

  public ApolloSubscriptionTerminatedException(String message) {
    super(message);
  }

  public ApolloSubscriptionTerminatedException(String message, Throwable cause) {
    super(message, cause);
  }
}
