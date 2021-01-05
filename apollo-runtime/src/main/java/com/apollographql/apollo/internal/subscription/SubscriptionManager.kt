package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener
import com.apollographql.apollo.subscription.SubscriptionManagerState

interface SubscriptionManager {
  /**
   * Starts provided subscription. Establishes connection to the subscription server if it was previously disconnected.
   *
   * @param subscription to start
   * @param callback to be called on result
   */
  fun <D : Operation.Data> subscribe(subscription: Subscription<D>, callback: Callback<D>)

  /**
   * Stops provided subscription. If there are no active subscriptions left, disconnects from the subscription server.
   *
   * @param subscription to stop
   */
  fun unsubscribe(subscription: Subscription<*>)

  /**
   * Returns the current state of subscription manager.
   *
   * @return current state
   */
  val state: SubscriptionManagerState

  /**
   * Adds new listener for subscription manager state changes.
   *
   * @param onStateChangeListener to be called when state changed
   */
  fun addOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener)

  /**
   * Removes listener for subscription manager state changes.
   *
   * @param onStateChangeListener to remove
   */
  fun removeOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener)

  /**
   * Put the [SubscriptionManager] in a connectible state. Does not necessarily open a web socket.
   */
  fun start()

  /**
   * Unsubscribe from all active subscriptions, and disconnect the web socket.
   */
  fun stop()

  /**
   * Reconnect the web socket. Use this together with SubscriptionConnectionParamsProvider if you need to update connectionParams.
   */
  fun reconnect()
  interface Callback<D : Operation.Data> {
    fun onResponse(response: SubscriptionResponse<D>)
    fun onError(error: ApolloSubscriptionException)
    fun onNetworkError(t: Throwable)
    fun onCompleted()
    fun onTerminated()
    fun onConnected()
  }
}