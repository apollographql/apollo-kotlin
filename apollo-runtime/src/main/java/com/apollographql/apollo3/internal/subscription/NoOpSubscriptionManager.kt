package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.Subscription
import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener
import com.apollographql.apollo.subscription.SubscriptionManagerState

class NoOpSubscriptionManager : SubscriptionManager {
  private val errorMessage = "No `SubscriptionTransport.Factory` found, please add one to your `ApolloClient` with `ApolloClient.Builder.subscriptionTransportFactory`"

  override fun <D : Operation.Data> subscribe(subscription: Subscription<D>, callback: SubscriptionManager.Callback<D>) {
    throw IllegalStateException(errorMessage)
  }

  override fun unsubscribe(subscription: Subscription<*>) {
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

  override val state: SubscriptionManagerState = SubscriptionManagerState.DISCONNECTED

  override fun addOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    throw IllegalStateException(errorMessage)
  }

  override fun removeOnStateChangeListener(onStateChangeListener: OnSubscriptionManagerStateChangeListener) {
    throw IllegalStateException(errorMessage)
  }
}
