package com.apollographql.apollo.subscription;

import org.jetbrains.annotations.NotNull;

/**
 * Provides instance of {@link SubscriptionConnectionParams} to be sent to the subscription server after connection is established.
 */
public interface SubscriptionConnectionParamsProvider {
  @NotNull
  SubscriptionConnectionParams provide();

  class Const implements SubscriptionConnectionParamsProvider {
    private final SubscriptionConnectionParams params;

    public Const(@NotNull SubscriptionConnectionParams params) {
      this.params = params;
    }

    @NotNull
    @Override
    public SubscriptionConnectionParams provide() {
      return params;
    }
  }
}
