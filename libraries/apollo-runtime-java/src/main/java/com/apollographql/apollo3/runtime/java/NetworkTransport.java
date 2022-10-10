package com.apollographql.apollo3.runtime.java;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import org.jetbrains.annotations.NotNull;

public interface NetworkTransport {
  <D extends Operation.Data> void execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback, ApolloDisposable disposable);
}
