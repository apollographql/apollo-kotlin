package com.apollographql.apollo3.subscription

/**
 *
 * `ApolloSubscriptionCall` is an abstraction for network transport layer that handles connection to the
 * subscription server. All updates related to the subscription pushed from the server will be delivered via [ ]
 *
 *
 */
interface SubscriptionTransport {
  /**
   * Opens connection to the subscription server
   */
  fun connect()

  /**
   * Disconnects from the subscription server.
   *
   * @param message to be sent as terminal event.
   */
  fun disconnect(message: OperationClientMessage)

  /**
   * Sends [OperationClientMessage] message to the subscription server.
   *
   * @param message to be sent to the server
   */
  fun send(message: OperationClientMessage)

  /**
   * Communicates responses from a subscription server.
   */
  interface Callback {
    /**
     * Gets called when connection with subscription server has been established.
     */
    fun onConnected()

    /**
     * Gets called when an unexpected exception occurs during communication to the server.
     *
     * @param t exception occurred during communication.
     */
    fun onFailure(t: Throwable)

    /**
     * Gets called when subscription server pushed new updates.
     *
     * @param message new message received from the server.
     */
    fun onMessage(message: OperationServerMessage)

    /**
     * Gets called when connection with subscription server is closed.
     */
    fun onClosed()
  }

  /**
   * Factory for creating new [SubscriptionTransport] transport layer.
   */
  interface Factory {
    /**
     * Creates and prepares a new [SubscriptionTransport].
     *
     * @param callback which will handle the transport communication events.
     * @return prepared [SubscriptionTransport]
     */
    fun create(callback: Callback): SubscriptionTransport
  }
}