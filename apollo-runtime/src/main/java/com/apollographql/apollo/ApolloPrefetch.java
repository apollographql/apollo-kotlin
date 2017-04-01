package com.apollographql.apollo;

import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Response;

public interface ApolloPrefetch extends Cancelable {

  void execute() throws ApolloException;

  @Nonnull ApolloPrefetch enqueue(@Nullable Callback callback);

  ApolloPrefetch clone();

  abstract class Callback {
    public abstract void onSuccess();

    public abstract void onFailure(@Nonnull ApolloException e);

    public void onHttpError(@Nonnull ApolloHttpException e) {
      onFailure(e);
      Response response = e.rawResponse();
      if (response != null) {
        response.close();
      }
    }

    public void onNetworkError(@Nonnull ApolloNetworkException e) {
      onFailure(e);
    }
  }
}
