package com.apollographql.apollo3.runtime.java.internal.ws;

import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WebSocketConnection {
  private static final boolean DEBUG = true;
  private static final long OPEN_TIMEOUT_MS = 10_000;

  private static final int CLOSE_NORMAL = 1000;
  private static final String MESSAGE_CLOSED = "__closed";

  private WebSocket.Factory webSocketFactory;
  private String serverUrl;
  private Map<String, String> headers;

  private WebSocket webSocket;
  private boolean isWebSocketOpen;

  private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

  public WebSocketConnection(WebSocket.Factory webSocketFactory, String serverUrl, Map<String, String> headers) {
    this.webSocketFactory = webSocketFactory;
    this.serverUrl = serverUrl;
    this.headers = headers;
  }

  public boolean open() {
    Request request = new Request.Builder()
        .url(serverUrl)
        .headers(toOkHttpHeaders(headers))
        .build();

    CountDownLatch openLatch = new CountDownLatch(1);

    webSocket = webSocketFactory.newWebSocket(request, new WebSocketListener() {
      @Override public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        if (DEBUG) System.out.println("WebSocketConnection.onOpen");
        isWebSocketOpen = true;
        openLatch.countDown();
      }

      @Override public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        if (DEBUG) System.out.println("WebSocketConnection.onMessage: " + text);
        messageQueue.add(text);
      }

      @Override public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        if (DEBUG) System.out.println("WebSocketConnection.onMessage: " + bytes.utf8());
        messageQueue.add(bytes.utf8());
      }

      @Override public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        if (DEBUG) System.out.println("WebSocketConnection.onFailure: " + t.getMessage());
        isWebSocketOpen = false;
        messageQueue.add(MESSAGE_CLOSED);
        openLatch.countDown();
      }

      @Override public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        if (DEBUG) System.out.println("WebSocketConnection.onClosing: " + code + " " + reason);
        isWebSocketOpen = false;
        messageQueue.add(MESSAGE_CLOSED);
      }

      @Override public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        if (DEBUG) System.out.println("WebSocketConnection.onClosed: " + code + " " + reason);
        isWebSocketOpen = false;
        messageQueue.add(MESSAGE_CLOSED);
      }
    });

    // Block until the web socket has opened (or failed)
    try {
      //noinspection ResultOfMethodCallIgnored
      openLatch.await(OPEN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
    } catch (InterruptedException ignored) {
    }
    return isWebSocketOpen;
  }

  public boolean isOpen() {
    return isWebSocketOpen;
  }

  public void close() {
    webSocket.close(CLOSE_NORMAL, null);
  }

  @Nullable
  public String receiveMessage() {
    if (!isWebSocketOpen) return null;
    try {
      String message = messageQueue.take();
      if (message.equals(MESSAGE_CLOSED)) {
        return null;
      }
      return message;
    } catch (InterruptedException e) {
      return null;
    }
  }

  public void send(String message) {
    if (!webSocket.send(message)) {

    }
  }

  private static Headers toOkHttpHeaders(Map<String, String> headers) {
    Headers.Builder builder = new Headers.Builder();
    for (Map.Entry<String, String> entry : headers.entrySet()) {
      builder.add(entry.getKey(), entry.getValue());
    }
    return builder.build();
  }
}
