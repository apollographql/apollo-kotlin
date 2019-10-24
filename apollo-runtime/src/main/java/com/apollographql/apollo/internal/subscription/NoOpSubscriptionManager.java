package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener;
import com.apollographql.apollo.subscription.SubscriptionManagerState;
import org.jetbrains.annotations.NotNull;

public final class NoOpSubscriptionManager implements SubscriptionManager {

  @Override
  public <T> void subscribe(@NotNull Subscription<?, T, ?> subscription, @NotNull Callback<T> callback) {
    throw new IllegalStateException("Subscription manager is not configured");
  }

  @Override
  public void unsubscribe(@NotNull Subscription<?, ?, ?> subscription) {
    throw new IllegalStateException("Subscription manager is not configured");
  }

  @Override
  public void start() {
    throw new IllegalStateException("Subscription manager is not configured");
  }

  @Override
  public void stop() {
    throw new IllegalStateException("Subscription manager is not configured");
  }

  @Override
  public SubscriptionManagerState getState() {
    return SubscriptionManagerState.DISCONNECTED;
  }

  @Override
  public void addOnStateChangeListener(@NotNull OnSubscriptionManagerStateChangeListener onStateChangeListener) {
    throw new IllegalStateException("Subscription manager is not configured");
  }

  @Override
  public void removeOnStateChangeListener(@NotNull OnSubscriptionManagerStateChangeListener onStateChangeListener) {
    throw new IllegalStateException("Subscription manager is not configured");
  }
}
