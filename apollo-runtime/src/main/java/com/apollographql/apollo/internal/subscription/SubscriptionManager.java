package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener;
import com.apollographql.apollo.subscription.SubscriptionManagerState;
import org.jetbrains.annotations.NotNull;

public interface SubscriptionManager {

  /**
   * Starts provided subscription. Establishes connection to the subscription server if it was previously disconnected.
   *
   * @param subscription to start
   * @param callback     to be called on result
   * @param <T>
   */
  <T> void subscribe(@NotNull Subscription<?, T, ?> subscription, @NotNull RealSubscriptionManager.Callback<T> callback);

  /**
   * Stops provided subscription. If there are no active subscriptions left, disconnects from the subscription server.
   *
   * @param subscription to stop
   */
  void unsubscribe(@NotNull Subscription<?, ?, ?> subscription);

  /**
   * Returns the current state of subscription manager.
   *
   * @return current state
   */
  SubscriptionManagerState getState();

  /**
   * Adds new listener for subscription manager state changes.
   *
   * @param onStateChangeListener to be called when state changed
   */
  void addOnStateChangeListener(@NotNull OnSubscriptionManagerStateChangeListener onStateChangeListener);

  /**
   * Removes listener for subscription manager state changes.
   *
   * @param onStateChangeListener to remove
   */
  void removeOnStateChangeListener(@NotNull OnSubscriptionManagerStateChangeListener onStateChangeListener);

  /**
   * Put the {@link SubscriptionManager} in a connectible state. Does not necessarily open a web
   * socket.
   */
  void start();

  /**
   * Unsubscribe from all active subscriptions, and disconnect the web socket.
   */
  void stop();

  /**
   * Reconnect the web socket. Use this together with SubscriptionConnectionParamsProvider if you need to update connectionParams.
   */
  void reconnect();

  interface Callback<T> {
    void onResponse(@NotNull SubscriptionResponse<T> response);

    void onError(@NotNull ApolloSubscriptionException error);

    void onNetworkError(@NotNull Throwable t);

    void onCompleted();

    void onTerminated();

    void onConnected();
  }
}
