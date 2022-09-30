package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.Query;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import org.jetbrains.annotations.Nullable;

public class DefaultApolloCall<D extends Operation.Data> implements ApolloCall<D> {
  private ApolloClient apolloClient;
  private Operation<D> operation;

  public DefaultApolloCall(ApolloClient apolloClient, Operation<D> operation) {
    this.apolloClient = apolloClient;
    this.operation = operation;
  }

  @Override public ApolloDisposable enqueue(@Nullable ApolloCallback<D> callback) {
    ApolloRequest apolloRequest = new ApolloRequest.Builder(operation)
        .build();

    return apolloClient.execute(apolloRequest, (ApolloCallback<? extends Query.Data>) callback);
  }
}
