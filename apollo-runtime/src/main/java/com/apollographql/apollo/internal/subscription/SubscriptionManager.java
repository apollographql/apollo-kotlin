package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;

import javax.annotation.Nonnull;

public interface SubscriptionManager {
  <T> void subscribe(@Nonnull Subscription<?, T, ?> subscription,
      @Nonnull RealSubscriptionManager.Callback<T> callback);

  void unsubscribe(@Nonnull Subscription<?, ?, ?> subscription);

  interface Callback<T> {
    void onResponse(@Nonnull Response<T> response);

    void onError(@Nonnull ApolloSubscriptionException error);

    void onNetworkError(@Nonnull Throwable t);

    void onCompleted();
  }
}
