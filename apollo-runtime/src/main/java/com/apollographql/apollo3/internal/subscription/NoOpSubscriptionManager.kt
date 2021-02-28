package com.apollographql.apollo3.internal.subscription

import com.apollographql.apollo3.api.Operation
import com.apollographql.apollo3.api.Subscription
import com.apollographql.apollo3.subscription.OnSubscriptionManagerStateChangeListener
import com.apollographql.apollo3.subscription.SubscriptionManagerState

class NoOpSubscriptionManager : SubscriptionManager {
  private val errorMessage = "No `SubscriptionTransport.Factory` found, please add one to your `ApolloClient` with `ApolloClient.Builder.subscriptionTransportFactory`"

  override fun <D : Subscription.Data> subscribe(subscription: Subscription<D>, callback: SubscriptionManager.Callback<D>) {
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
