package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.response.ScalarTypeAdapters;
import com.apollographql.apollo.subscription.OperationClientMessage;
import com.apollographql.apollo.subscription.OperationServerMessage;
import com.apollographql.apollo.subscription.SubscriptionTransport;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.internal.subscription.RealSubscriptionManager.idForSubscription;
import static com.google.common.truth.Truth.assertThat;

public class SubscriptionManagerTest {
  private MockSubscriptionTransportFactory subscriptionTransportFactory;
  private RealSubscriptionManager subscriptionManager;
  private MockSubscription subscription1 = new MockSubscription("MockSubscription1");
  private MockSubscription subscription2 = new MockSubscription("MockSubscription2");

  @Before public void setUp() throws Exception {
    subscriptionTransportFactory = new MockSubscriptionTransportFactory();
    subscriptionManager = new RealSubscriptionManager(new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap()),
        subscriptionTransportFactory, Collections.<String, Object>emptyMap(), new MockExecutor());
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

    subscriptionTransportFactory.subscriptionTransport.disconnectCountDownLatch.awaitOrThrowWithTimeout
        (RealSubscriptionManager.INACTIVITY_TIMEOUT + 800, TimeUnit.MILLISECONDS);
    assertThat(subscriptionTransportFactory.subscriptionTransport.disconnectMessage).isInstanceOf(OperationClientMessage.Terminate.class);
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.DISCONNECTED);
    assertThat(subscriptionManager.timer.tasks).isEmpty();
  }

  @Test public void reconnect() throws Exception {
    subscriptionManager.subscribe(subscription1, new SubscriptionManagerCallbackAdapter<Operation.Data>());
    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionManager.unsubscribe(subscription1);

    subscriptionTransportFactory.subscriptionTransport.disconnectCountDownLatch.awaitOrThrowWithTimeout
        (RealSubscriptionManager.INACTIVITY_TIMEOUT + 800, TimeUnit.MILLISECONDS);
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.DISCONNECTED);

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
    subscriptionTransportFactory.subscriptionTransport.disconnectCountDownLatch.awaitOrThrowWithTimeout
        (RealSubscriptionManager.CONNECTION_ACKNOWLEDGE_TIMEOUT + 800, TimeUnit.MILLISECONDS);
    assertThat(subscriptionTransportFactory.subscriptionTransport.disconnectMessage).isInstanceOf(OperationClientMessage.Terminate.class);
    assertThat(subscriptionManager.state).isEqualTo(RealSubscriptionManager.State.DISCONNECTED);
    assertThat(subscriptionManager.timer.tasks).isEmpty();
    assertThat(subscriptionManager.subscriptions).isEmpty();
  }

  @Test public void disconnectedOnTransportFailure() throws Exception {
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

  @Test public void unsubscribeOnComplete() throws Exception {
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

  @Test public void unsubscribeOnError() throws Exception {
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

  @Test public void notifyOnData() throws Exception {
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback1 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1);

    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.Data(idForSubscription(subscription1),
        Collections.<String, Object>emptyMap()));

    assertThat(subscriptionManagerCallback1.response).isNotNull();
  }

  @Test public void duplicateSubscriptions() throws Exception {
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback1 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback1);
    SubscriptionManagerCallbackAdapter<Operation.Data> subscriptionManagerCallback2 = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription1, subscriptionManagerCallback2);

    assertThat(subscriptionManagerCallback2.error).hasMessage("Already subscribed");
  }

  private static final class MockSubscriptionTransportFactory implements SubscriptionTransport.Factory {
    MockSubscriptionTransport subscriptionTransport;
    SubscriptionTransport.Callback callback;

    @Override public SubscriptionTransport create(@Nonnull SubscriptionTransport.Callback callback) {
      this.callback = callback;
      return subscriptionTransport = new MockSubscriptionTransport();
    }
  }

  private static final class MockSubscriptionTransport implements SubscriptionTransport {
    volatile OperationClientMessage lastSentMessage;
    volatile boolean connected;
    volatile OperationClientMessage disconnectMessage;
    NamedCountDownLatch disconnectCountDownLatch;

    @Override public void connect() {
      connected = true;
      disconnectCountDownLatch = new NamedCountDownLatch("Disconnect", 1);
    }

    @Override public void disconnect(OperationClientMessage message) {
      connected = false;
      disconnectMessage = message;
      disconnectCountDownLatch.countDown();
    }

    @Override public void send(OperationClientMessage message) {
      lastSentMessage = message;
    }
  }

  private static final class MockExecutor implements Executor {
    @Override public void execute(@Nonnull Runnable command) {
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

    @Nonnull @Override public OperationName name() {
      return new OperationName() {
        @Override public String name() {
          return "SomeSubscription";
        }
      };
    }

    @Nonnull @Override public String operationId() {
      return operationId;
    }
  }

  private static class SubscriptionManagerCallbackAdapter<T> implements SubscriptionManager.Callback<T> {
    volatile Response<T> response;
    volatile ApolloSubscriptionException error;
    volatile Throwable networkError;
    volatile boolean completed;

    @Override public void onResponse(@Nonnull Response<T> response) {
      this.response = response;
    }

    @Override public void onError(@Nonnull ApolloSubscriptionException error) {
      this.error = error;
    }

    @Override public void onNetworkError(@Nonnull Throwable t) {
      networkError = t;
    }

    @Override public void onCompleted() {
      completed = true;
    }
  }

  private static class NamedCountDownLatch extends CountDownLatch {
    private String name;

    NamedCountDownLatch(String name, int count) {
      super(count);
      this.name = name;
    }

    public String name() {
      return name;
    }

    public void awaitOrThrowWithTimeout(long timeout, TimeUnit timeUnit)
        throws InterruptedException, TimeoutException {
      if (!this.await(timeout, timeUnit)) {
        throw new TimeoutException("Time expired before latch, " + this.name() + " count went to zero.");
      }
    }
  }
}
