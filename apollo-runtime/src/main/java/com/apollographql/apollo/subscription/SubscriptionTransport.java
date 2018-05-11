package com.apollographql.apollo.subscription;

import org.jetbrains.annotations.NotNull;

/**
 * <p>{@code ApolloSubscriptionCall} is an abstraction for network transport layer that handles connection to the
 * subscription server. All updates related to the subscription pushed from the server will be delivered via {@link
 * Callback}<p/>
 */
public interface SubscriptionTransport {

  /**
   * Opens connection to the subscription server
   */
  void connect();

  /**
   * Disconnects from the subscription server.
   *
   * @param message to be sent as terminal event.
   */
  void disconnect(OperationClientMessage message);

  /**
   * Sends {@link OperationClientMessage} message to the subscription server.
   *
   * @param message to be sent to the server
   */
  void send(OperationClientMessage message);

  /**
   * Communicates responses from a subscription server.
   */
  interface Callback {
    /**
     * Gets called when connection with subscription server has been established.
     */
    void onConnected();

    /**
     * Gets called when an unexpected exception occurs during communication to the server.
     *
     * @param t exception occurred during communication.
     */
    void onFailure(Throwable t);

    /**
     * Gets called when subscription server pushed new updates.
     *
     * @param message new message received from the server.
     */
    void onMessage(OperationServerMessage message);
  }

  /**
   * Factory for creating new {@link SubscriptionTransport} transport layer.
   */
  interface Factory {
    /**
     * Creates and prepares a new {@link SubscriptionTransport}.
     *
     * @param callback which will handle the transport communication events.
     * @return prepared {@link SubscriptionTransport}
     */
    SubscriptionTransport create(@NotNull Callback callback);
  }
}
