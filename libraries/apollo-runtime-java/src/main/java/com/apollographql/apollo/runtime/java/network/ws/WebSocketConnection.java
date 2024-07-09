package com.apollographql.apollo.runtime.java.network.ws;

import com.apollographql.apollo.api.http.HttpHeader;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WebSocketConnection {
  private static final boolean DEBUG = false;
  private static final long OPEN_TIMEOUT_MS = 10_000;

  private static final int CLOSE_NORMAL = 1000;
  private static final String MESSAGE_CLOSED = "__closed";

  private WebSocket.Factory webSocketFactory;
  private String serverUrl;
  private List<HttpHeader> headers;

  private WebSocket webSocket;
  private boolean isWebSocketOpen;

  private BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

  public WebSocketConnection(WebSocket.Factory webSocketFactory, String serverUrl, List<HttpHeader> headers) {
    this.webSocketFactory = webSocketFactory;
    this.serverUrl = serverUrl;
    this.headers = headers;
  }

  public void open() throws Throwable {
    CountDownLatch openLatch = new CountDownLatch(1);
    Request request = new Request.Builder()
        .url(serverUrl)
        .headers(toOkHttpHeaders(headers))
        .build();
    final Throwable[] onFailureThrowable = {null};
    webSocket = webSocketFactory.newWebSocket(request, new WebSocketListener() {
      @Override public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        if (DEBUG) System.out.println("onOpen");
        isWebSocketOpen = true;
        openLatch.countDown();
      }

      @Override public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        if (DEBUG) System.out.println("onMessage: " + text);
        messageQueue.add(text);
      }

      @Override public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        if (DEBUG) System.out.println("onMessage: " + bytes.utf8());
        messageQueue.add(bytes.utf8());
      }

      @Override public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        if (DEBUG) System.out.println("onFailure: " + t.getMessage());
        isWebSocketOpen = false;
        messageQueue.add(MESSAGE_CLOSED);
        onFailureThrowable[0] = t;
        openLatch.countDown();
      }

      @Override public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        if (DEBUG) System.out.println("onClosing: " + code + " " + reason);
        isWebSocketOpen = false;
        messageQueue.add(MESSAGE_CLOSED);
      }

      @Override public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        if (DEBUG) System.out.println("onClosed: " + code + " " + reason);
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
    if (onFailureThrowable[0] != null) {
      throw onFailureThrowable[0];
    }
  }

  public boolean isOpen() {
    return isWebSocketOpen;
  }

  public void close() {
    if (DEBUG) System.out.println("close");
    webSocket.close(CLOSE_NORMAL, null);
  }

  /**
   * @param timeoutMs the timeout in milliseconds or -1 for no timeout
   */
  @Nullable
  public String receive(long timeoutMs) {
    if (!isWebSocketOpen) return null;
    try {
      String message = timeoutMs == -1L ? messageQueue.take() : messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
      if (message.equals(MESSAGE_CLOSED)) {
        return null;
      }
      return message;
    } catch (InterruptedException e) {
      return null;
    }
  }

  public void send(String message) {
    if (DEBUG) System.out.println("send: " + message);
    if (!webSocket.send(message)) {
      isWebSocketOpen = false;
    }
  }

  public void send(ByteString message) {
    if (DEBUG) System.out.println("send: " + message.utf8());
    if (!webSocket.send(message)) {
      isWebSocketOpen = false;
    }
  }

  private static Headers toOkHttpHeaders(List<HttpHeader> headers) {
    Headers.Builder builder = new Headers.Builder();
    for (HttpHeader header : headers) {
      builder.add(header.getName(), header.getValue());
    }
    return builder.build();
  }
}
