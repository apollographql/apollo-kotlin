package com.apollographql.apollo.subscription;

/**
 * Subscription manager state.
 */
public enum SubscriptionManagerState {

  /**
   * Indicates there is no active connection to the subscription server.
   */
  DISCONNECTED,

  /**
   * Indicates manager is trying to connect to the subscription server.
   */
  CONNECTING,

  /**
   * Indicates there is active connection with the subscription server, waiting for GraphQL session to be initialized.
   */
  CONNECTED,

  /**
   * Indicates there is active connection with the subscription server and GraphQL session has been initialized.
   * Subscriptions are ready to use.
   */
  ACTIVE,

  /**
   * Indicates user initiates manager to stop GraphQL session and disconnect from the subscription server.
   */
  STOPPING,

  /**
   * Indicates user stopped GraphQL session and disconnected from the subscription server.
   */
  STOPPED
}
