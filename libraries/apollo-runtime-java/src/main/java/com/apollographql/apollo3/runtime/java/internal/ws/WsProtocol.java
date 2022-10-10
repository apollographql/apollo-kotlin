package com.apollographql.apollo3.runtime.java.internal.ws;

import com.apollographql.apollo3.api.Adapters;
import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.json.BufferedSinkJsonWriter;
import com.apollographql.apollo3.api.json.BufferedSourceJsonReader;
import okio.Buffer;
import okio.ByteString;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;

public abstract class WsProtocol {
  private WebSocketConnection webSocketConnection;
  protected Listener listener;

  public WsProtocol(WebSocketConnection webSocketConnection, Listener listener) {
    this.webSocketConnection = webSocketConnection;
    this.listener = listener;
  }

  abstract void connectionInit();

  abstract void handleServerMessage(Map<String, Object> messageMap);

  abstract <D extends Operation.Data> void startOperation(ApolloRequest<D> request);

  abstract <D extends Operation.Data> void stopOperation(ApolloRequest<D> request);


  protected void sendMessageMap(Map<String, Object> messageMap, WsFrameType frameType) {
    switch (frameType) {
      case Text:
        sendMessageMapText(messageMap);
        break;
      case Binary:
        sendMessageMapBinary(messageMap);
        break;
    }
  }

  protected void sendMessageMapText(Map<String, Object> messageMap) {
    webSocketConnection.send(toJsonString(messageMap));
  }

  protected void sendMessageMapBinary(Map<String, Object> messageMap) {
    webSocketConnection.send(toJsonByteString(messageMap));
  }

  /**
   * Receive a new WebMessage message as a `Map<String, Any?>`. Messages that aren't Json objects are ignored and the method will block
   * until the next message. Returns null if the connection is closed.
   */
  @Nullable
  protected Map<String, Object> receiveMessageMap() {
    while (true) {
      String messageJson = webSocketConnection.receive();
      if (messageJson == null) {
        return null;
      }
      Map<String, Object> map = toMessageMap(messageJson);
      if (map != null) {
        return map;
      }
    }
  }

  protected void run() {
    while (true) {
      Map<String, Object> messageMap = receiveMessageMap();
      if (messageMap == null) {
        // Connection closed
        listener.networkError(new IOException("Connection closed"));
        return;
      }
      handleServerMessage(messageMap);
    }
  }

  private static String toJsonString(Map<String, Object> messageMap) {
    Buffer buffer = new Buffer();
    BufferedSinkJsonWriter writer = new BufferedSinkJsonWriter(buffer);
    try {
      Adapters.AnyAdapter.toJson(writer, CustomScalarAdapters.Empty, messageMap);
    } catch (IOException ignored) {
    }
    return buffer.readUtf8();
  }

  private ByteString toJsonByteString(Map<String, Object> messageMap) {
    Buffer buffer = new Buffer();
    BufferedSinkJsonWriter writer = new BufferedSinkJsonWriter(buffer);
    try {
      Adapters.AnyAdapter.toJson(writer, CustomScalarAdapters.Empty, messageMap);
    } catch (IOException ignored) {
    }
    return buffer.readByteString();
  }

  private static Map<String, Object> toMessageMap(String messageJson) {
    try {
      //noinspection unchecked
      return (Map<String, Object>) Adapters.AnyAdapter.fromJson(new BufferedSourceJsonReader(new Buffer().writeUtf8(messageJson)), CustomScalarAdapters.Empty);
    } catch (Exception e) {
      return null;
    }
  }

  public void close() {
    webSocketConnection.close();
  }


  interface Listener {
    /**
     * A response was received. payload might contain "errors". For subscriptions, several responses might be received.
     */
    void operationResponse(String id, Map<String, Object> payload);

    /**
     * An error was received in relation to an operation
     */
    void operationError(String id, Map<String, Object> payload);

    /**
     * An operation is complete
     */
    void operationComplete(String id);

    /**
     * A general error was received. A general error is a protocol error that doesn't have an operation id. If you have an operation id, use
     * `operationError` instead.
     */
    void generalError(Map<String, Object> payload);

    /**
     * A network error occurred A network error is terminal
     */
    void networkError(Throwable cause);
  }

  public enum WsFrameType {
    Text,
    Binary
  }

  public interface Factory {
    String getName();

    WsProtocol create(WebSocketConnection webSocketConnection, Listener listener);
  }
}
