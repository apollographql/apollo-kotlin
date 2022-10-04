package com.apollographql.apollo3.runtime.java.internal.ws;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import okhttp3.WebSocket;

import java.util.Map;

public abstract class WsProtocol {
  public interface Listener {
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


  private WebSocket webSocket;
  private Listener listener;

  public WsProtocol(WebSocket webSocket, Listener listener) {
    this.webSocket = webSocket;
    this.listener = listener;
  }

  abstract void connectionInit();

  abstract void handleServerMessage(Map<String, Object> messageMap);

  abstract <D extends Operation.Data> void startOperation(ApolloRequest<D> request);

  abstract <D extends Operation.Data> void stopOperation(ApolloRequest<D> request);
}
