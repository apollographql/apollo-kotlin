package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Subscription;

import javax.annotation.Nonnull;

public final class NoOpSubscriptionManager implements SubscriptionManager {

  @Override
  public <T> void subscribe(@Nonnull Subscription<?, T, ?> subscription, @Nonnull Callback<T> callback) {
    throw new IllegalStateException("Subscription manager is not configured");
  }

  @Override public void unsubscribe(@Nonnull Subscription<?, ?, ?> subscription) {
    throw new IllegalStateException("Subscription manager is not configured");
  }
}
