package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.cache.http.HttpCacheControl;
import com.apollographql.android.cache.normalized.CacheControl;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloCall<T> extends Cancelable {

  @Nonnull Response<T> execute() throws IOException;

  void enqueue(@Nullable Callback<T> callback);

  @Nonnull ApolloWatcher<T> watcher();

  @Nonnull ApolloCall<T> httpCacheControl(@Nonnull HttpCacheControl httpCacheControl);

  @Nonnull ApolloCall<T> cacheControl(@Nonnull CacheControl cacheControl);

  @Nonnull ApolloCall<T> clone();

  interface Callback<T> {
    void onResponse(@Nonnull Response<T> response);

    void onFailure(@Nonnull Exception e);
  }

  interface Factory {
    <D extends Operation.Data, T, V extends Operation.Variables> ApolloCall<T> newCall(
        @Nonnull Operation<D, T, V> operation);

    <D extends Operation.Data, T, V extends Operation.Variables> ApolloPrefetch prefetch(
        @Nonnull Operation<D, T, V> operation);
  }
}
