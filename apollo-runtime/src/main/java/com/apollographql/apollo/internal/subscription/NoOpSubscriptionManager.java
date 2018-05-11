package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Subscription;

import org.jetbrains.annotations.NotNull;

public final class NoOpSubscriptionManager implements SubscriptionManager {

  @Override
  public <T> void subscribe(@NotNull Subscription<?, T, ?> subscription, @NotNull Callback<T> callback) {
    throw new IllegalStateException("Subscription manager is not configured");
  }

  @Override public void unsubscribe(@NotNull Subscription<?, ?, ?> subscription) {
    throw new IllegalStateException("Subscription manager is not configured");
  }
}
