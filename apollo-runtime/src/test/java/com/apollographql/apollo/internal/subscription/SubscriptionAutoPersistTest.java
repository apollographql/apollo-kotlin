package com.apollographql.apollo.internal.subscription;

import com.apollographql.apollo.api.CustomScalarTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Response;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.ResponseFieldMapper;
import com.apollographql.apollo.api.internal.ResponseFieldMarshaller;
import com.apollographql.apollo.api.internal.ResponseReader;
import com.apollographql.apollo.cache.normalized.ApolloStore;
import com.apollographql.apollo.cache.normalized.internal.ResponseNormalizer;
import com.apollographql.apollo.subscription.OperationClientMessage;
import com.apollographql.apollo.subscription.OperationServerMessage;
import com.apollographql.apollo.subscription.SubscriptionConnectionParams;
import com.apollographql.apollo.subscription.SubscriptionConnectionParamsProvider;
import com.apollographql.apollo.subscription.SubscriptionManagerState;
import com.apollographql.apollo.subscription.SubscriptionTransport;
import kotlin.jvm.functions.Function0;
import okio.BufferedSource;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;

import static com.google.common.truth.Truth.assertThat;

public class SubscriptionAutoPersistTest {
  private MockSubscriptionTransportFactory subscriptionTransportFactory;
  private RealSubscriptionManager subscriptionManager;
  private MockSubscription subscription = new MockSubscription("MockSubscription");
  private SubscriptionManagerCallbackAdapter<Operation.Data> callbackAdapter;

  @Before public void setUp() {
    subscriptionTransportFactory = new MockSubscriptionTransportFactory();
    subscriptionManager = new RealSubscriptionManager(new ScalarTypeAdapters(Collections.<ScalarType, CustomScalarTypeAdapter<?>>emptyMap()),
        subscriptionTransportFactory, new SubscriptionConnectionParamsProvider.Const(new SubscriptionConnectionParams()),
        new MockExecutor(), -1, new Function0<ResponseNormalizer<Map<String, Object>>>() {
      @Override public ResponseNormalizer<Map<String, Object>> invoke() {
        return ApolloStore.NO_APOLLO_STORE.networkResponseNormalizer();
      }
    }, true);
    assertThat(subscriptionTransportFactory.subscriptionTransport).isNotNull();
    assertThat(subscriptionManager.state).isEqualTo(SubscriptionManagerState.DISCONNECTED);

    callbackAdapter = new SubscriptionManagerCallbackAdapter<>();
    subscriptionManager.subscribe(subscription, callbackAdapter);
    subscriptionTransportFactory.callback.onConnected();
    subscriptionTransportFactory.callback.onMessage(new OperationServerMessage.ConnectionAcknowledge());
    assertStartMessage(false);
  }

  @Test public void success() {
    final UUID subscriptionId = new ArrayList<>(subscriptionManager.subscriptions.keySet()).get(0);
    subscriptionTransportFactory.callback.onMessage(
        new OperationServerMessage.Data(subscriptionId.toString(), Collections.<String, Object>emptyMap())
    );
    assertThat(callbackAdapter.response).isNotNull();
  }

  @Test public void protocolNegotiationErrorNotFound() {
    final UUID subscriptionId = new ArrayList<>(subscriptionManager.subscriptions.keySet()).get(0);
    subscriptionTransportFactory.callback.onMessage(
        new OperationServerMessage.Error(
            subscriptionId.toString(),
            Collections.singletonMap("message", RealSubscriptionManager.PROTOCOL_NEGOTIATION_ERROR_NOT_FOUND)
        )
    );
    assertStartMessage(true);
  }

  @Test public void protocolNegotiationErrorNotSupported() {
    final UUID subscriptionId = new ArrayList<>(subscriptionManager.subscriptions.keySet()).get(0);
    subscriptionTransportFactory.callback.onMessage(
        new OperationServerMessage.Error(
            subscriptionId.toString(),
            Collections.singletonMap("message", RealSubscriptionManager.PROTOCOL_NEGOTIATION_ERROR_NOT_SUPPORTED)
        )
    );
    assertStartMessage(true);
  }

  @Test public void unknownError() {
    final UUID subscriptionId = new ArrayList<>(subscriptionManager.subscriptions.keySet()).get(0);
    subscriptionTransportFactory.callback.onMessage(
        new OperationServerMessage.Error(
            subscriptionId.toString(),
            Collections.singletonMap("meh", "¯\\_(ツ)_/¯")
        )
    );
    assertThat(callbackAdapter.error).isInstanceOf(ApolloSubscriptionServerException.class);
    assertThat(((ApolloSubscriptionServerException) callbackAdapter.error).errorPayload).containsEntry("meh", "¯\\_(ツ)_/¯");
  }

  private void assertStartMessage(boolean isWriteDocument) {
    final UUID subscriptionId = new ArrayList<>(subscriptionManager.subscriptions.keySet()).get(0);
    if (isWriteDocument) {
      assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage.toJsonString()).isEqualTo(
          ""
              + "{\"id\":\"" + subscriptionId.toString() + "\","
              + "\"type\":\"start\","
              + "\"payload\":{"
              + "\"variables\":{},"
              + "\"operationName\":\"SomeSubscription\","
              + "\"query\":\"subscription{\\ncommentAdded(repoFullName:\\\"repo\\\"){\\n__typename\\nid\\ncontent\\n}\\n}\","
              + "\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"MockSubscription\"}}}}"
      );
    } else {
      assertThat(subscriptionTransportFactory.subscriptionTransport.lastSentMessage.toJsonString()).isEqualTo(
          ""
              + "{\"id\":\"" + subscriptionId.toString() + "\","
              + "\"type\":\"start\","
              + "\"payload\":{"
              + "\"variables\":{},"
              + "\"operationName\":\"SomeSubscription\","
              + "\"extensions\":{\"persistedQuery\":{\"version\":1,\"sha256Hash\":\"MockSubscription\"}}}}"
      );
    }
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

    @Override public void connect() {
    }

    @Override public void disconnect(OperationClientMessage message) {
    }

    @Override public void send(OperationClientMessage message) {
      lastSentMessage = message;
    }
  }

  private static final class MockExecutor implements Executor {
    @Override public void execute(@NotNull Runnable command) {
      command.run();
    }
  }

  private static final class MockSubscription implements Subscription<Operation.Data, Operation.Variables> {
    final String operationId;

    MockSubscription(String operationId) {
      this.operationId = operationId;
    }

    @Override public String queryDocument() {
      return "subscription{\ncommentAdded(repoFullName:\"repo\"){\n__typename\nid\ncontent\n}\n}";
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper<Data> responseFieldMapper() {
      return new ResponseFieldMapper<Data>() {
        @Override public Data map(ResponseReader responseReader) {
          return new Data() {
            @Override public ResponseFieldMarshaller marshaller() {
              throw new UnsupportedOperationException();
            }
          };
        }
      };
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

    @NotNull @Override public Response<Data> parse(@NotNull BufferedSource source) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public Response<Data> parse(@NotNull BufferedSource source, @NotNull ScalarTypeAdapters scalarTypeAdapters) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public Response parse(@NotNull ByteString byteString) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public Response parse(@NotNull ByteString byteString, @NotNull ScalarTypeAdapters scalarTypeAdapters) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public ByteString composeRequestBody(
        boolean autoPersistQueries,
        boolean withQueryDocument,
        @NotNull ScalarTypeAdapters scalarTypeAdapters) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public ByteString composeRequestBody(@NotNull ScalarTypeAdapters scalarTypeAdapters) {
      throw new UnsupportedOperationException();
    }

    @NotNull @Override public ByteString composeRequestBody() {
      throw new UnsupportedOperationException();
    }
  }

  private static class SubscriptionManagerCallbackAdapter<D extends Operation.Data> implements SubscriptionManager.Callback<D> {
    volatile SubscriptionResponse<D> response;
    volatile ApolloSubscriptionException error;
    volatile Throwable networkError;
    volatile boolean completed;
    volatile boolean terminated;
    volatile boolean connected;

    @Override public void onResponse(@NotNull SubscriptionResponse<D> response) {
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

    @Override public void onConnected() {
      connected = true;
    }
  }
}
