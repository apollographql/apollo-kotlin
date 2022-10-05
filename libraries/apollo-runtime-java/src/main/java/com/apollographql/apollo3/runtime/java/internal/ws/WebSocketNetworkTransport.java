package com.apollographql.apollo3.runtime.java.internal.ws;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WebSocketNetworkTransport {
  private WebSocket.Factory webSocketFactory;
  private String serverUrl;
  private WsProtocol.Factory wsProtocolFactory;
  private List<HttpHeader> headers;

  private Map<UUID, List<ApolloCallback<?>>> activeSubscriptions = new HashMap<>();
  private WsProtocol wsProtocol;

  public WebSocketNetworkTransport(
      WebSocket.Factory webSocketFactory,
      WsProtocol.Factory wsProtocolFactory,
      String serverUrl,
      List<HttpHeader> headers
  ) {
    this.webSocketFactory = webSocketFactory;
    this.serverUrl = serverUrl;
    this.wsProtocolFactory = wsProtocolFactory;
    this.headers = headers;
  }

  private WsProtocol.Listener listener = new WsProtocol.Listener() {
    @Override public void operationResponse(String id, Map<String, Object> payload) {
      //TODO
    }

    @Override public void operationError(String id, Map<String, Object> payload) {
      //TODO
    }

    @Override public void operationComplete(String id) {
      //TODO
    }

    @Override public void generalError(Map<String, Object> payload) {
      //TODO
    }

    @Override public void networkError(Throwable cause) {
      //TODO
    }
  };

  public <D extends Operation.Data> void execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback, ApolloDisposable disposable) {
    List<ApolloCallback<?>> callbacks = activeSubscriptions.get(request.getRequestUuid());
    if (callbacks == null) {
      callbacks = new ArrayList<>();
      activeSubscriptions.put(request.getRequestUuid(), callbacks);
      subscribe(request);
    }
    callbacks.add(callback);
  }

  private <D extends Operation.Data> void subscribe(ApolloRequest<D> request) {
    ensureWebSocketOpen();
    wsProtocol.startOperation(request);
  }

  private void ensureWebSocketOpen() {
    if (wsProtocol == null) {
      WebSocketConnection webSocket = openWebSocket();
      if (webSocket == null) {
        // TODO
      } else {
        initWsProtocol(webSocket);
      }
    }
  }

  private WebSocketConnection openWebSocket() {
    List<HttpHeader> headers = new ArrayList<>(this.headers);
    if (headers.stream().noneMatch(it -> it.getName().equals("Sec-WebSocket-Protocol"))) {
      headers.add(new HttpHeader("Sec-WebSocket-Protocol", wsProtocolFactory.getName()));
    }
    WebSocketConnection webSocketConnection = new WebSocketConnection(webSocketFactory, serverUrl, headers);
    boolean openSuccess = webSocketConnection.open();
    if (!openSuccess) {
      // TODO
      return null;
    }
    return webSocketConnection;
  }

  private void initWsProtocol(WebSocketConnection webSocket) {
    wsProtocol = wsProtocolFactory.create(webSocket, listener);
    wsProtocol.connectionInit();
  }
}
