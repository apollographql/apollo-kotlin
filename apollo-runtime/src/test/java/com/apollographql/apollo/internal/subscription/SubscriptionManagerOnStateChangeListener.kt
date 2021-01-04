package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.subscription.OnSubscriptionManagerStateChangeListener;
import com.apollographql.apollo.subscription.SubscriptionManagerState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.truth.Truth.assertThat;

class SubscriptionManagerOnStateChangeListener implements OnSubscriptionManagerStateChangeListener {
    private final List<SubscriptionManagerState> stateNotifications = new ArrayList<>();

    @Override
    public void onStateChange(SubscriptionManagerState fromState, SubscriptionManagerState toState) {
      synchronized (stateNotifications) {
        stateNotifications.add(toState);
        stateNotifications.notify();
      }
    }

    void awaitState(SubscriptionManagerState state, long timeout, TimeUnit timeUnit) throws InterruptedException {
      synchronized (stateNotifications) {
        if (stateNotifications.contains(state)) {
          return;
        }

        stateNotifications.clear();
        stateNotifications.wait(timeUnit.toMillis(timeout));

        assertThat(stateNotifications).contains(state);
      }
    }
  }