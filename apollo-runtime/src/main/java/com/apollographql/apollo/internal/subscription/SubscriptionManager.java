package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.Subscription;

import org.jetbrains.annotations.NotNull;

public interface SubscriptionManager {
  <T> void subscribe(@NotNull Subscription<?, T, ?> subscription,
      @NotNull RealSubscriptionManager.Callback<T> callback);

  void unsubscribe(@NotNull Subscription<?, ?, ?> subscription);

  /**
   * Put the {@link SubscriptionManager} in a connectible state. Does not necessarily open a web
   * socket.
   */
  void start();

  /**
   * Unsubscribe from all active subscriptions, and disconnect the web socket.
   */
  void stop();

  interface Callback<T> {
    void onResponse(@NotNull Response<T> response);

    void onError(@NotNull ApolloSubscriptionException error);

    void onNetworkError(@NotNull Throwable t);

    void onCompleted();

    void onTerminated();
  }
}
