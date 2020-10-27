package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener
import com.apollographql.apollo.subscription.SubscriptionManagerState

class NoOpSubscriptionManager : SubscriptionManager {
  val errorMessage = "No `SubscriptionTransport.Factory` found, please add one to your `ApolloClient` with `ApolloClient.Builder.subscriptionTransportFactory`"

  override fun <T> subscribe(subscription: Subscription<*, T, *>, callback: SubscriptionManager.Callback<T>) {
    throw IllegalStateException(errorMessage)
  }

  override fun unsubscribe(subscription: Subscription<*, *, *>) {
    throw IllegalStateException(errorMessage)
  }

  override fun start() {
    throw IllegalStateException(errorMessage)
  }

  override fun stop() {
    throw IllegalStateException(errorMessage)
  }

  override fun reconnect() {
  }

  override fun getState(): SubscriptionManagerState {
    return SubscriptionManagerState.DISCONNECTED
  }

  override fun addOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    throw IllegalStateException(errorMessage)
  }

  override fun removeOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    throw IllegalStateException(errorMessage)
  }
}