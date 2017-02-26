package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloCall {

  @Nonnull <T extends Operation.Data> Response<T> execute() throws IOException;

  @Nonnull <T extends Operation.Data> ApolloCall enqueue(@Nullable Callback<T> callback);

  @Nonnull ApolloCall network();

  @Nonnull ApolloCall cache();

  @Nonnull ApolloCall networkBeforeStale();

  void cancel();

  @Nonnull ApolloCall clone();

  interface Callback<T extends Operation.Data> {
    void onResponse(@Nonnull Response<T> response);

    void onFailure(@Nonnull Exception e);
  }

  interface Factory<R> {
    @Nonnull <T extends Operation> R newCall(@Nonnull T operation);
  }
}
