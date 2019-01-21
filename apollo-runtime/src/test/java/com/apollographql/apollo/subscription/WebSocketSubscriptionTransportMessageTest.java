package com.apollographql.apollo.subscription;

import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.api.internal.UnmodifiableMapBuilder;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static com.google.common.truth.Truth.assertThat;

public class WebSocketSubscriptionTransportMessageTest {
  private MockWebSocketFactory webSocketFactory;
  private WebSocketSubscriptionTransport subscriptionTransport;
  private MockSubscriptionTransportCallback transportCallback;

  @Before public void setUp() throws Exception {
    webSocketFactory = new MockWebSocketFactory();
    transportCallback = new MockSubscriptionTransportCallback();

    WebSocketSubscriptionTransport.Factory factory = new WebSocketSubscriptionTransport.Factory("wss://localhost/", webSocketFactory);
    subscriptionTransport = (WebSocketSubscriptionTransport) factory.create(transportCallback);

    subscriptionTransport.connect();
    assertThat(webSocketFactory.webSocket).isNotNull();
  }

  @Test public void connectionInit() {
    subscriptionTransport.send(new OperationClientMessage.Init(Collections.<String, Object>emptyMap()));
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("{\"type\":\"connection_init\"}");

    subscriptionTransport.send(
        new OperationClientMessage.Init(
            new UnmodifiableMapBuilder<String, Object>()
                .put("param1", true)
                .put("param2", "value")
                .build()
        )
    );
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("{\"type\":\"connection_init\",\"payload\":{\"param1\":true,\"param2\":\"value\"}}");
  }

  @Test public void startSubscription() {
    subscriptionTransport.send(new OperationClientMessage.Start("subscriptionId", new MockSubscription(),
        new ScalarTypeAdapters(Collections.<ScalarType, CustomTypeAdapter>emptyMap())));
    assertThat(webSocketFactory.webSocket.lastSentMessage)
        .isEqualTo("{\"id\":\"subscriptionId\",\"type\":\"start\",\"payload\":{\"query\":\"subscription{commentAdded{id  name}\",\"variables\":{},\"operationName\":\"SomeSubscription\"}}");
  }

  @Test public void stopSubscription() {
    subscriptionTransport.send(new OperationClientMessage.Stop("subscriptionId"));
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("{\"id\":\"subscriptionId\",\"type\":\"stop\"}");
  }

  @Test public void terminateSubscription() {
    subscriptionTransport.send(new OperationClientMessage.Terminate());
    assertThat(webSocketFactory.webSocket.lastSentMessage).isEqualTo("{\"type\":\"connection_terminate\"}");
  }

  @Test public void connectionAcknowledge() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"connection_ack\"}");
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.ConnectionAcknowledge.class);
  }

  @SuppressWarnings("unchecked")
  @Test public void data() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"data\",\"id\":\"subscriptionId\",\"payload\":{\"data\":{\"commentAdded\":{\"__typename\":\"Comment\",\"id\":10,\"content\":\"test10\"}}}}");
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Data.class);
    assertThat(((OperationServerMessage.Data) transportCallback.lastMessage).id).isEqualTo("subscriptionId");
    assertThat((Map<String, Object>) ((Map<String, Object>) ((OperationServerMessage.Data) transportCallback.lastMessage).payload.get("data")).get("commentAdded"))
        .containsExactlyEntriesIn(new UnmodifiableMapBuilder<String, Object>()
            .put("__typename", "Comment")
            .put("id", BigDecimal.valueOf(10))
            .put("content", "test10")
            .build()
        );
  }

  @Test public void connectionError() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"connection_error\",\"payload\":{\"message\":\"Connection Error\"}}");
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.ConnectionError.class);
    assertThat(((OperationServerMessage.ConnectionError) transportCallback.lastMessage).payload).containsExactly("message", "Connection Error");
  }

  @Test public void error() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"error\", \"id\":\"subscriptionId\", \"payload\":{\"message\":\"Error\"}}");
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Error.class);
    assertThat(((OperationServerMessage.Error) transportCallback.lastMessage).id).isEqualTo("subscriptionId");
    assertThat(((OperationServerMessage.Error) transportCallback.lastMessage).payload).containsExactly("message", "Error");
  }

  @Test public void complete() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"complete\", \"id\":\"subscriptionId\"}");
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Complete.class);
    assertThat(((OperationServerMessage.Complete) transportCallback.lastMessage).id).isEqualTo("subscriptionId");
  }

  @Test public void unsupported() {
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"unsupported\"}");
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Unsupported.class);
    assertThat(((OperationServerMessage.Unsupported) transportCallback.lastMessage).rawMessage).isEqualTo("{\"type\":\"unsupported\"}");

    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"unsupported");
    assertThat(transportCallback.lastMessage).isInstanceOf(OperationServerMessage.Unsupported.class);
    assertThat(((OperationServerMessage.Unsupported) transportCallback.lastMessage).rawMessage).isEqualTo("{\"type\":\"unsupported");
  }

  private static final class MockWebSocketFactory implements WebSocket.Factory {
    MockWebSocket webSocket;

    @Override public WebSocket newWebSocket(@NotNull Request request, @NotNull WebSocketListener listener) {
      if (webSocket != null) {
        throw new IllegalStateException("already initialized");
      }
      return webSocket = new MockWebSocket(request, listener);
    }
  }

  private static final class MockWebSocket implements WebSocket {
    final Request request;
    final WebSocketListener listener;
    String lastSentMessage;

    MockWebSocket(Request request, WebSocketListener listener) {
      this.request = request;
      this.listener = listener;
      this.listener.onOpen(this, new okhttp3.Response.Builder()
          .request(request)
          .protocol(Protocol.HTTP_1_0)
          .code(200)
          .message("Ok")
          .build()
      );
    }

    @Override public Request request() {
      return request;
    }

    @Override public long queueSize() {
      throw new UnsupportedOperationException();
    }

    @Override public boolean send(@NotNull String text) {
      lastSentMessage = text;
      return true;
    }

    @Override public boolean send(@NotNull ByteString bytes) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean close(int code, @Nullable String reason) {
      throw new UnsupportedOperationException();
    }

    @Override public void cancel() {
      throw new UnsupportedOperationException();
    }
  }

  private static final class MockSubscriptionTransportCallback implements SubscriptionTransport.Callback {
    OperationServerMessage lastMessage;

    @Override public void onConnected() {
    }

    @Override public void onFailure(Throwable t) {
    }

    @Override public void onMessage(OperationServerMessage message) {
      lastMessage = message;
    }

    @Override public void onClosed() {
    }
  }

  private static final class MockSubscription implements Subscription<Operation.Data, Operation.Data, Operation.Variables> {
    @Override public String queryDocument() {
      return "subscription{commentAdded{id\n  name\n}";
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper<Operation.Data> responseFieldMapper() {
      throw new UnsupportedOperationException();
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
      return "someId";
    }
  }
}
