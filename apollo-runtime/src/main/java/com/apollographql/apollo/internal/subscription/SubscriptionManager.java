package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;

import org.jetbrains.annotations.NotNull;

public interface SubscriptionManager {
  <T> void subscribe(@NotNull Subscription<?, T, ?> subscription,
      @NotNull RealSubscriptionManager.Callback<T> callback);

  void unsubscribe(@NotNull Subscription<?, ?, ?> subscription);

  interface Callback<T> {
    void onResponse(@NotNull Response<T> response);

    void onError(@NotNull ApolloSubscriptionException error);

    void onNetworkError(@NotNull Throwable t);

    void onCompleted();
  }
}
