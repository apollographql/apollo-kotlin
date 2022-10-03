package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import org.jetbrains.annotations.NotNull;

public class DefaultApolloCall<D extends Operation.Data> implements ApolloCall<D> {
  private ApolloClient apolloClient;
  private Operation<D> operation;

  public DefaultApolloCall(ApolloClient apolloClient, Operation<D> operation) {
    this.apolloClient = apolloClient;
    this.operation = operation;
  }

  @Override public ApolloDisposable enqueue(@NotNull ApolloCallback<D> callback) {
    ApolloRequest<D> apolloRequest = new ApolloRequest.Builder<>(operation)
        .build();

    return apolloClient.execute(apolloRequest, callback);
  }
}
