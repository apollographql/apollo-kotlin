package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.ScalarTypeAdapters;
import com.apollographql.apollo.subscription.OperationClientMessage;
import com.apollographql.apollo.subscription.OperationServerMessage;
import com.apollographql.apollo.subscription.SubscriptionTransport;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static com.apollographql.apollo.internal.subscription.RealSubscriptionManager.idForSubscription;
import static com.google.common.truth.Truth.assertThat;

public class SubscriptionManagerTest {
  private final long connectionHeartbeatTimeoutMs = TimeUnit.SECONDS.toMillis(1);
  private MockSubscriptionTransportFactory subscriptionTransportFactory;
  private RealSubscriptionManager subscriptionManager;
  private MockSubscription subscription1 = new MockSubscription("MockSubscription1");
  private MockSubscription subscription2 = new MockSubscription("MockSubscription2");
  private SubscriptionManagerOnStateChangeListener onStateChangeListener = new SubscriptionManagerOnStateChangeListener();

  @Before public void setUp() {
    subscriptionTransportFactory = new MockSubscriptionTransportFactory();
    subscriptionManager = new RealSubscriptionManager(new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        subscriptionTransportFactory, Collections.<String, Object>emptyMap(), new MockExecutor(), connectionHeartbeatTimeoutMs);
    subscriptionManager.addOnStateChangeListener(onStateChangeListener);
    assertThat(subscriptionTransportFactory.subscriptionTransport).isNotNull();
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.DISCONNECTED);
  }

  @Test public void connecting() {
    subscriptionManager.subscribe(subscription1, new SubscriptionManagerCallbackAdapter<Operation.Data>());

    assertThat(subscriptionTransportFactory.subscriptionTransport).isNotNull();
    assertThat(subscriptionTransportFactory.subscriptionTransport.connected).isTrue();
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isNull();
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.CONNECTING);

    subscriptionManager.subscribe(subscription2, new SubscriptionManagerCallbackAdapter<Operation.Data>());
    assertThat(subscriptionTransportFactory.subscriptionTransport.connected).isTrue();
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isNull();
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.CONNECTING);

    assertThat(subscriptionManager.subscriptions).hasSize(2);
    assertThat(subscriptionManager.subscriptions.get(idForSubscription(subscription1))).isNotNull();
    assertThat(subscriptionManager.subscriptions.get(idForSubscription(subscription2))).isNotNull();

    assertThat(subscriptionManager.timer.tasks).isEmpty();
  }

  @Test public void connected() {
    subscriptionManager.subscribe(subscription1, new SubscriptionManagerCallbackAdapter<Operation.Data>());
    subscriptionTransportFactory.callback.onConnected();
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.CONNECTED);
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isInstanceOf(OperationClientMessage.Init.class);
    assertThat(subscriptionManager.timer.tasks).containsKey(RealSubscriptionManager.CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID);
  }

  @Test public void active() {
    subscriptionManager.subscribe(subscription1, new SubscriptionManagerCallbackAdapter<Operation.Data>());
    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.ACTIVE);
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isInstanceOf(OperationClientMessage.Start.class);
    assertThat(((OperationClientMessage.Start) subscriptionTransportFactory.subscriptionTransport.lastSentMessage).subscriptionId).isEqualTo(idForSubscription(subscription1));
    assertThat(subscriptionManager.timer.tasks).isEmpty();
  }

  @Test public void disconnected() throws Exception {
    subscriptionManager.subscribe(subscription1, new SubscriptionManagerCallbackAdapter<Operation.Data>());
    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionManager.unsubscribe(subscription1);

    assertThat(subscriptionManager.subscriptions).isEmpty();
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isInstanceOf(OperationClientMessage.Stop.class);
    assertThat(((OperationClientMessage.Stop) subscriptionTransportFactory.subscriptionTransport.lastSentMessage).subscriptionId).isEqualTo(idForSubscription(subscription1));

    assertThat(subscriptionManager.timer.tasks).containsKey(RealSubscriptionManager.INACTIVITY_TIMEOUT_TIMER_TASK_ID);

    onStateChangeListener.awaitState(RealSubscriptionManager.State.DISCONNECTED, RealSubscriptionManager
        .INACTIVITY_TIMEOUT + 800, TimeUnit.MILLISECONDS);

    assertThat(subscriptionTransportFactory.subscriptionTransport.disconnectMessage).isInstanceOf(OperationClientMessage.Terminate.class);
    assertThat(subscriptionManager.timer.tasks).isEmpty();
  }

  @Test public void reconnect() throws Exception {
    subscriptionManager.subscribe(subscription1, new SubscriptionManagerCallbackAdapter<Operation.Data>());
    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionManager.unsubscribe(subscription1);

    onStateChangeListener.awaitState(RealSubscriptionManager.State.DISCONNECTED, RealSubscriptionManager
        .INACTIVITY_TIMEOUT + 800, TimeUnit.MILLISECONDS);

    subscriptionManager.subscribe(subscription2, new SubscriptionManagerCallbackAdapter<Operation.Data>());
    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());

    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.ACTIVE);
    assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage).isInstanceOf(OperationClientMessage.Start.class);
    assertThat(((OperationClientMessage.Start) subscriptionTransportFactory.subscriptionTransport.lastSentMessage).subscriptionId).isEqualTo(idForSubscription(subscription2));
    assertThat(subscriptionManager.timer.tasks).isEmpty();
  }

  @Test public void disconnectedOnConnectionAcknowledgeTimeout() throws Exception {
    subscriptionManager.subscribe(subscription1, new SubscriptionManagerCallbackAdapter<Operation.Data>());
    subscriptionTransportFactory.callback.onConnected();

    assertThat(subscriptionManager.timer.tasks).containsKey(RealSubscriptionManager.CONNECTION_ACKNOWLEDGE_TIMEOUT_TIMER_TASK_ID);

    onStateChangeListener.awaitState(RealSubscriptionManager.State.DISCONNECTED, RealSubscriptionManager
        .CONNECTION_ACKNOWLEDGE_TIMEOUT + 800, TimeUnit.MILLISECONDS);

    assertThat(subscriptionTransportFactory.subscriptionTransport.disconnectMessage).isInstanceOf(OperationClientMessage.Terminate.class);
    assertThat(subscriptionManager.timer.tasks).isEmpty();
    assertThat(subscriptionManager.subscriptions).isEmpty();
  }

  @Test public void disconnectedOnTransportFailure() {
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback1 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1);
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback2 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription2, subscriptionManagerCallback2);
    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionTransportFactory.callback.onFailure(new UnsupportedOperationException());
    assertThat(subscriptionManagerCallback1.networkError).isInstanceOf(UnsupportedOperationException.class);
    assertThat(subscriptionManagerCallback2.networkError).isInstanceOf(UnsupportedOperationException.class);
    assertThat(subscriptionTransportFactory.subscriptionTransport.disconnectMessage).isInstanceOf(OperationClientMessage.Terminate.class);
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.DISCONNECTED);
    assertThat(subscriptionManager.timer.tasks).isEmpty();
    assertThat(subscriptionManager.subscriptions).isEmpty();
  }

  @Test public void unsubscribeOnComplete() {
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback1 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1);
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback2 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription2, subscriptionManagerCallback2);

    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.Complete(idForSubscription(subscription1)));
    assertThat(subscriptionManagerCallback1.completed).isTrue();

    assertThat(subscriptionManager.subscriptions).hasSize(1);
    assertThat(subscriptionManager.subscriptions).containsKey(idForSubscription(subscription2));
    assertThat(subscriptionManagerCallback2.completed).isFalse();
  }

  @Test public void unsubscribeOnError() {
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback1 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1);
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback2 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription2, subscriptionManagerCallback2);

    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.Error(idForSubscription(subscription1),
        new UnmodifiableMapBuilder<String, Object>().put("key1", "value1").put("key2", "value2").build()));

    assertThat(subscriptionManagerCallback1.error).isInstanceOf(ApolloSubscriptionServerException.class);
    assertThat(((ApolloSubscriptionServerException) subscriptionManagerCallback1.error).errorPayload).containsEntry("key1", "value1");
    assertThat(((ApolloSubscriptionServerException) subscriptionManagerCallback1.error).errorPayload).containsEntry("key2", "value2");

    assertThat(subscriptionManager.subscriptions).hasSize(1);
    assertThat(subscriptionManager.subscriptions).containsKey(idForSubscription(subscription2));
    assertThat(subscriptionManagerCallback2.completed).isFalse();
  }

  @Test public void notifyOnData() {
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback1 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1);

    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.Data(idForSubscription(subscription1),
        Collections.<String, Object>emptyMap()));

    assertThat(subscriptionManagerCallback1.response).isNotNull();
  }

  @Test public void duplicateSubscriptions() {
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback1 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1);
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback2 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback2);

    assertThat(subscriptionManagerCallback2.error).hasMessage("Already subscribed");
  }

  @Test public void reconnectingAfterHeartbeatTimeout() throws Exception {
    subscriptionManager.subscribe(subscription1, new SubscriptionManagerCallbackAdapter<Operation.Data>());

    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionKeepAlive());

    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.ACTIVE);
    assertThat(subscriptionManager.timer.tasks).containsKey(RealSubscriptionManager.CONNECTION_KEEP_ALIVE_TIMEOUT_TIMER_TASK_ID);

    onStateChangeListener.awaitState(RealSubscriptionManager.State.DISCONNECTED, connectionHeartbeatTimeoutMs + 800, TimeUnit.MILLISECONDS);
    onStateChangeListener.awaitState(RealSubscriptionManager.State.CONNECTING, 800, TimeUnit.MILLISECONDS);
  }

  @Test public void connectionTerminated() {
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback);

    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());

    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.ACTIVE);

    subscriptionTransportFactory.callback.onClosed();
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.DISCONNECTED);
    assertThat(subscriptionManagerCallback.terminated).isTrue();
  }

  private static final class MockSubscriptionTransportFactory implements SubscriptionTransport.Factory {
    MockSubscriptionTransport subscriptionTransport;
    SubscriptionTransport.Callback callback;

    @Override public SubscriptionTransport create(@NotNull SubscriptionTransport.Callback callback) {
      this.callback = callback;
      return subscriptionTransport = new MockSubscriptionTransport();
    }
  }

  private static final class MockSubscriptionTransport implements SubscriptionTransport {
    volatile OperationClientMessage lastSentMessage;
    volatile boolean connected;
    volatile OperationClientMessage disconnectMessage;

    @Override public void connect() {
      connected = true;
    }

    @Override public void disconnect(OperationClientMessage message) {
      connected = false;
      disconnectMessage = message;
    }

    @Override public void send(OperationClientMessage message) {
      lastSentMessage = message;
    }
  }

  private static class SubscriptionManagerOnStateChangeListener implements RealSubscriptionManager.OnStateChangeListener {
    private final List<RealSubscriptionManager.State> stateNotifications = new ArrayList<>();

    @Override
    public void onStateChange(RealSubscriptionManager.State fromState, RealSubscriptionManager.State toState) {
      synchronized (stateNotifications) {
        stateNotifications.add(toState);
        stateNotifications.notify();
      }
    }

    void awaitState(RealSubscriptionManager.State state, long timeout, TimeUnit timeUnit) throws InterruptedException {
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

  private static final class MockExecutor implements Executor {
    @Override public void execute(@NotNull Runnable command) {
      command.run();
    }
  }

  private static final class MockSubscription implements Subscription<Operation.Data, Operation.Data, Operation.Variables> {
    final String operationId;

    MockSubscription(String operationId) {
      this.operationId = operationId;
    }

    @Override public String queryDocument() {
      return "subscription {\n  commentAdded(repoFullName: \"repo\") {\n    __typename\n    id\n    content\n  }\n}";
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper<Data> responseFieldMapper() {
      return new ResponseFieldMapper<Data>() {
        @Override public Data map(ResponseReader responseReader) {
          return new Operation.Data() {
            @Override public ResponseFieldMarshaller marshaller() {
              throw new UnsupportedOperationException();
            }
          };
        }
      };
    }

    @Override public Operation.Data wrapData(Data data) {
      return data;
    }

    @NotNull @Override public OperationName name() {
      return new OperationName() {
        @Override public String name() {
          return "SomeSubscription";
        }
      };
    }

    @NotNull @Override public String operationId() {
      return operationId;
    }
  }

  private static class SubscriptionManagerCallbackAdapter<T> implements SubscriptionManager.Callback<T> {
    volatile Response<T> response;
    volatile ApolloSubscriptionException error;
    volatile Throwable networkError;
    volatile boolean completed;
    volatile boolean terminated;

    @Override public void onResponse(@NotNull Response<T> response) {
      this.response = response;
    }

    @Override public void onError(@NotNull ApolloSubscriptionException error) {
      this.error = error;
    }

    @Override public void onNetworkError(@NotNull Throwable t) {
      networkError = t;
    }

    @Override public void onCompleted() {
      completed = true;
    }

    @Override public void onTerminated() {
      terminated = true;
    }
  }
}
