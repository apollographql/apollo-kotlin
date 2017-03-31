package com.apollographql.apollo;

import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.exception.ApolloHttpException;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.internal.util.Cancelable;

import java.io.IOException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ApolloPrefetch extends Cancelable {

  void execute() throws ApolloException;

  @Nonnull ApolloPrefetch enqueue(@Nullable Callback callback);

  ApolloPrefetch clone();

  abstract class Callback {
    public abstract void onSuccess();

    public abstract void onFailure(@Nonnull ApolloException e);

    public void onHttpError(@Nonnull ApolloHttpException e) {
      onFailure(e);
    }

    public void onNetworkError(@Nonnull ApolloNetworkException e) {
      onFailure(e);
    }
  }
}
