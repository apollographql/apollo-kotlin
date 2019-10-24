package com.apollographql.apollo.subscription;

/**
 * Represents a callback for subscription manager state changes.
 */
public interface OnSubscriptionManagerStateChangeListener {
  /**
   * Called when subscription manager state changed.
   *
   * @param fromState previous subscription manager state
   * @param toState   new subscription manager state
   */
  void onStateChange(SubscriptionManagerState fromState, SubscriptionManagerState toState);
}
