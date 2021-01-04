package com.apollographql.apollo.subscription

/**
 * Represents a callback for subscription manager state changes.
 */
interface OnSubscriptionManagerStateChangeListener {
  /**
   * Called when subscription manager state changed.
   *
   * @param fromState previous subscription manager state
   * @param toState   new subscription manager state
   */
  fun onStateChange(fromState: SubscriptionManagerState?, toState: SubscriptionManagerState?)
}