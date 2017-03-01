package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloCall<T extends Operation.Data> {

  @Nonnull Response<T> execute() throws IOException;

  @Nonnull ApolloCall<T> enqueue(@Nullable Callback<T> callback);

  @Nonnull ApolloCall<T> network();

  @Nonnull ApolloCall<T> cache();

  @Nonnull ApolloCall<T> networkBeforeStale();

  @Nonnull ApolloCall<T> clone();

  void cancel();

  interface Callback<T extends Operation.Data> {
    void onResponse(@Nonnull Response<T> response);

    void onFailure(@Nonnull Exception e);
  }

  interface Factory {
    <D extends Operation.Data, V extends Operation.Variables> ApolloCall<D> newCall(@Nonnull Operation<D, V> operation);

    <D extends Operation.Data, V extends Operation.Variables> ApolloPrefetch prefetch(
        @Nonnull Operation<D, V> operation);
  }
}
