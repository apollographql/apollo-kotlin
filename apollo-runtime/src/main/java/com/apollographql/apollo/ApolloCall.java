package com.apollographql.apollo;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.cache.http.HttpCacheControl;
import com.apollographql.apollo.cache.normalized.CacheControl;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.ApolloParseException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloCall<T> extends Cancelable {

  @Nonnull Response<T> execute() throws ApolloException;

  void enqueue(@Nullable Callback<T> callback);

  @Nonnull ApolloWatcher<T> watcher();

  @Nonnull ApolloCall<T> httpCacheControl(@Nonnull HttpCacheControl httpCacheControl);

  @Nonnull ApolloCall<T> cacheControl(@Nonnull CacheControl cacheControl);

  @Nonnull ApolloCall<T> clone();

  abstract class Callback<T> {
    public abstract void onResponse(@Nonnull Response<T> response);

    public abstract void onFailure(@Nonnull ApolloException e);

    public void onHttpError(@Nonnull ApolloHttpException e) {
      onFailure(e);
      okhttp3.Response response = e.rawResponse();
      if (response != null) {
        response.close();
      }
    }

    public void onNetworkError(@Nonnull ApolloNetworkException e) {
      onFailure(e);
    }

    public void onParseError(@Nonnull ApolloParseException e) {
      onFailure(e);
    }
  }

  interface Factory {
    <D extends Operation.Data, T, V extends Operation.Variables> ApolloCall<T> newCall(
        @Nonnull Operation<D, T, V> operation);

    <D extends Operation.Data, T, V extends Operation.Variables> ApolloPrefetch prefetch(
        @Nonnull Operation<D, T, V> operation);
  }
}
