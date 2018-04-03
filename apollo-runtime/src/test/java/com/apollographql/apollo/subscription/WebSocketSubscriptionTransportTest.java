package com.apollographql.apollo.subscription;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;

import static com.google.common.truth.Truth.assertThat;
import static junit.framework.TestCase.fail;

public class WebSocketSubscriptionTransportTest {
  private Request webSocketRequest;
  private MockWebSocketFactory webSocketFactory;
  private WebSocketSubscriptionTransport subscriptionTransport;

  @Before public void setUp() throws Exception {
    webSocketRequest = new Request.Builder().url("wss://localhost").build();
    webSocketFactory = new MockWebSocketFactory();
    WebSocketSubscriptionTransport.Factory factory = new WebSocketSubscriptionTransport.Factory("wss://localhost/", webSocketFactory);
    subscriptionTransport = (WebSocketSubscriptionTransport) factory.create(new SubscriptionTransport.Callback() {
      @Override public void onConnected() {
      }

      @Override public void onFailure(Throwable t) {
      }

      @Override public void onMessage(OperationServerMessage message) {
      }
    });
  }

  @Test public void connect() {
    assertThat(subscriptionTransport.webSocket.get()).isNull();
    assertThat(subscriptionTransport.webSocketListener.get()).isNull();

    subscriptionTransport.connect();
    assertThat(subscriptionTransport.webSocket.get()).isNotNull();
    assertThat(subscriptionTransport.webSocketListener.get()).isNotNull();

    assertThat(webSocketFactory.request.header("Sec-WebSocket-Protocol")).isEqualTo("graphql-ws");
    assertThat(webSocketFactory.request.header("Cookie")).isEqualTo("");
  }

  @Test public void disconnect() {
    subscriptionTransport.connect();
    assertThat(subscriptionTransport.webSocket.get()).isNotNull();
    assertThat(subscriptionTransport.webSocketListener.get()).isNotNull();

    subscriptionTransport.disconnect(new OperationClientMessage.Terminate());
    assertThat(subscriptionTransport.webSocket.get()).isNull();
    assertThat(subscriptionTransport.webSocketListener.get()).isNull();
  }

  @Test public void send() {
    try {
      subscriptionTransport.send(new OperationClientMessage.Init(Collections.<String, Object>emptyMap()));
      fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
      // expected
    }

    subscriptionTransport.connect();
    subscriptionTransport.send(new OperationClientMessage.Init(Collections.<String, Object>emptyMap()));
    subscriptionTransport.disconnect(new OperationClientMessage.Terminate());

    try {
      subscriptionTransport.send(new OperationClientMessage.Init(Collections.<String, Object>emptyMap()));
      fail("expected IllegalStateException");
    } catch (IllegalStateException expected) {
      // expected
    }
  }

  @Test public void subscriptionTransportCallback() {
    final AtomicBoolean callbackConnected = new AtomicBoolean();
    final AtomicReference<Throwable> callbackFailure = new AtomicReference<>();
    final AtomicReference<OperationServerMessage> callbackMessage = new AtomicReference<>();
    subscriptionTransport = new WebSocketSubscriptionTransport(webSocketRequest, webSocketFactory, new SubscriptionTransport.Callback() {
      @Override public void onConnected() {
        callbackConnected.set(true);
      }

      @Override public void onFailure(Throwable t) {
        callbackFailure.set(t);
      }

      @Override public void onMessage(OperationServerMessage message) {
        callbackMessage.set(message);
      }
    });
    subscriptionTransport.connect();
    webSocketFactory.webSocket.listener.onMessage(webSocketFactory.webSocket, "{\"type\":\"connection_ack\"}");
    webSocketFactory.webSocket.listener.onFailure(webSocketFactory.webSocket, new UnsupportedOperationException(), null);

    assertThat(callbackConnected.get()).isTrue();
    assertThat(callbackMessage.get()).isInstanceOf(OperationServerMessage.ConnectionAcknowledge.class);
    assertThat(callbackFailure.get()).isInstanceOf(UnsupportedOperationException.class);
  }

  private static final class MockWebSocketFactory implements WebSocket.Factory {
    Request request;
    MockWebSocket webSocket;

    @Override public WebSocket newWebSocket(@Nonnull Request request, @Nonnull WebSocketListener listener) {
      if (webSocket != null) {
        throw new IllegalStateException("already initialized");
      }
      this.request = request;
      return webSocket = new MockWebSocket(request, listener);
    }
  }

  private static final class MockWebSocket implements WebSocket {
    final Request request;
    final WebSocketListener listener;
    String lastSentMessage;
    boolean closed;

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

    @Override public boolean send(@Nonnull String text) {
      lastSentMessage = text;
      return true;
    }

    @Override public boolean send(@Nonnull ByteString bytes) {
      throw new UnsupportedOperationException();
    }

    @Override public boolean close(int code, @Nullable String reason) {
      return closed = true;
    }

    @Override public void cancel() {
      throw new UnsupportedOperationException();
    }
  }
}
