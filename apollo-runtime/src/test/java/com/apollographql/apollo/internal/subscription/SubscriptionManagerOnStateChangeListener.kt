package com.apollographql.apollo.internal.subscription

import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener
import com.apollographql.apollo.subscription.SubscriptionManagerState
import com.google.common.truth.Truth
import java.util.ArrayList
import java.util.concurrent.TimeUnit

internal class SubscriptionManagerOnStateChangeListener : OnSubscriptionManagerStateChangeListener {
  private val stateNotifications: MutableList<SubscriptionManagerState?> = ArrayList()
  val lock = Object()

  override fun onStateChange(fromState: SubscriptionManagerState?, toState: SubscriptionManagerState?) {
    synchronized(lock) {
      stateNotifications.add(toState)
      lock.notify()
    }
  }

  @kotlin.jvm.Throws(InterruptedException::class)
  fun awaitState(state: SubscriptionManagerState?, timeout: Long, timeUnit: TimeUnit) {
    synchronized(lock) {
      if (stateNotifications.contains(state)) {
        return
      }
      stateNotifications.clear()
      lock.wait(timeUnit.toMillis(timeout))
      Truth.assertThat(stateNotifications).contains(state)
    }
  }
}