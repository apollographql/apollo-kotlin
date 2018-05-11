package com.apollographql.apollo.subscription;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicReference;

import org.jetbrains.annotations.NotNull;

import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

/**
 * <p>{@link SubscriptionTransport} implementation based on {@link WebSocket}.<p/>
 */
public final class WebSocketSubscriptionTransport implements SubscriptionTransport {
  private final Request webSocketRequest;
  private final WebSocket.Factory webSocketConnectionFactory;
  private final Callback callback;
  final AtomicReference<WebSocket> webSocket = new AtomicReference<>();
  final AtomicReference<WebSocketListener> webSocketListener = new AtomicReference<>();

  WebSocketSubscriptionTransport(Request webSocketRequest, WebSocket.Factory webSocketConnectionFactory,
      Callback callback) {
    this.webSocketRequest = webSocketRequest;
    this.webSocketConnectionFactory = webSocketConnectionFactory;
    this.callback = callback;
  }

  @Override
  public void connect() {
    WebSocketListener webSocketListener = new WebSocketListener(this);
    if (!this.webSocketListener.compareAndSet(null, webSocketListener)) {
      throw new IllegalStateException("Already connected");
    }
    webSocket.set(webSocketConnectionFactory.newWebSocket(webSocketRequest, webSocketListener));
  }

  @Override
  public void disconnect(OperationClientMessage message) {
    WebSocket socket = webSocket.getAndSet(null);

    if (socket != null) {
      socket.close(1001, message.toJsonString());
    }

    release();
  }

  @Override
  public void send(OperationClientMessage message) {
    WebSocket socket = webSocket.get();
    if (socket == null) {
      throw new IllegalStateException("Not connected");
    }
    socket.send(message.toJsonString());
  }

  void onOpen() {
    callback.onConnected();
  }

  void onMessage(OperationServerMessage message) {
    callback.onMessage(message);
  }

  void onFailure(Throwable t) {
    try {
      callback.onFailure(t);
    } finally {
      release();
    }
  }

  void release() {
    WebSocketListener socketListener = webSocketListener.getAndSet(null);
    if (socketListener != null) {
      socketListener.release();
    }
    webSocket.set(null);
  }

  static final class WebSocketListener extends okhttp3.WebSocketListener {
    final WeakReference<WebSocketSubscriptionTransport> delegateRef;

    WebSocketListener(WebSocketSubscriptionTransport delegate) {
      delegateRef = new WeakReference<>(delegate);
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
      WebSocketSubscriptionTransport delegate = delegateRef.get();
      if (delegate != null) {
        delegate.onOpen();
      }
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
      WebSocketSubscriptionTransport delegate = delegateRef.get();
      if (delegate != null) {
        OperationServerMessage message = OperationServerMessage.fromJsonString(text);
        delegate.onMessage(message);
      }
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
      WebSocketSubscriptionTransport delegate = delegateRef.get();
      if (delegate != null) {
        delegate.onFailure(t);
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      WebSocketSubscriptionTransport delegate = delegateRef.get();
      if (delegate != null) {
        delegate.release();
      }
    }

    @Override
    public void onClosed(WebSocket webSocket, int code, String reason) {
      WebSocketSubscriptionTransport delegate = delegateRef.get();
      if (delegate != null) {
        delegate.release();
      }
    }

    void release() {
      delegateRef.clear();
    }
  }

  public static final class Factory implements SubscriptionTransport.Factory {
    private final Request webSocketRequest;
    private final WebSocket.Factory webSocketConnectionFactory;

    public Factory(@NotNull String webSocketUrl, @NotNull WebSocket.Factory webSocketConnectionFactory) {
      this.webSocketRequest = new Request.Builder()
          .url(checkNotNull(webSocketUrl, "webSocketUrl == null"))
          .addHeader("Sec-WebSocket-Protocol", "graphql-ws")
          .addHeader("Cookie", "")
          .build();
      this.webSocketConnectionFactory = checkNotNull(webSocketConnectionFactory, "webSocketConnectionFactory == null");
    }

    @Override
    public SubscriptionTransport create(@NotNull Callback callback) {
      checkNotNull(callback, "callback == null");
      return new WebSocketSubscriptionTransport(webSocketRequest, webSocketConnectionFactory, callback);
    }
  }
}
