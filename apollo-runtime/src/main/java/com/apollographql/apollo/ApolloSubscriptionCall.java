package com.apollographql.apollo;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.exception.ApolloException;
import com.apollographql.apollo.internal.util.Cancelable;

import javax.annotation.Nonnull;

public interface ApolloSubscriptionCall<T> extends Cancelable {
  void execute(@Nonnull Callback<T> callback);

  ApolloSubscriptionCall<T> clone();

  interface Factory {
    <D extends Subscription.Data, T, V extends Subscription.Variables> ApolloSubscriptionCall<T> subscribe(
        @Nonnull Subscription<D, T, V> subscription);
  }

  interface Callback<T> {

    void onResponse(@Nonnull Response<T> response);

    void onFailure(@Nonnull ApolloException e);

    void onCompleted();
  }
}
