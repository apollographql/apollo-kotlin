package com.apollographql.apollo3.runtime.java.internal.ws;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.ApolloResponse;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.Operations;
import com.apollographql.apollo3.api.http.HttpHeader;
import com.apollographql.apollo3.api.json.JsonReader;
import com.apollographql.apollo3.api.json.MapJsonReader;
import com.apollographql.apollo3.exception.ApolloNetworkException;
import com.apollographql.apollo3.exception.SubscriptionOperationException;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

public class WebSocketNetworkTransport {
  private WebSocket.Factory webSocketFactory;
  private String serverUrl;
  private WsProtocol.Factory wsProtocolFactory;
  private List<HttpHeader> headers;
  private Executor executor;

  // TODO make this synchronized
  private Map<String, SubscriptionInfo> activeSubscriptions = new HashMap<>();
  private WsProtocol wsProtocol;

  public WebSocketNetworkTransport(
      WebSocket.Factory webSocketFactory,
      WsProtocol.Factory wsProtocolFactory,
      String serverUrl,
      List<HttpHeader> headers,
      Executor executor
  ) {
    this.webSocketFactory = webSocketFactory;
    this.serverUrl = serverUrl;
    this.wsProtocolFactory = wsProtocolFactory;
    this.headers = headers;
    this.executor = executor;
  }

  public <D extends Operation.Data> void execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback, ApolloDisposable disposable) {
    SubscriptionInfo subscriptionInfo = new SubscriptionInfo(request, callback, disposable);
    String id = request.getRequestUuid().toString();
    activeSubscriptions.put(id, subscriptionInfo);
    disposable.addListener(() -> {
      if (activeSubscriptions.containsKey(id)) {
        // Caller has cancelled the subscription by calling ApolloDisposable.dispose()
        if (wsProtocol != null) wsProtocol.stopOperation(request);
        disposeSubscription(id);
      }
    });
    if (ensureWsProtocolRunning()) {
      wsProtocol.startOperation(request);
    }
  }

  private boolean ensureWsProtocolRunning() {
    if (wsProtocol == null) {
      WebSocketConnection webSocket;
      try {
        webSocket = openWebSocket();
      } catch (Throwable e) {
        listener.networkError(e);
        return false;
      }
      initWsProtocol(webSocket);
      executor.execute(() -> wsProtocol.run());
    }
    return true;
  }

  private WebSocketConnection openWebSocket() throws Throwable {
    List<HttpHeader> headers = new ArrayList<>(this.headers);
    if (headers.stream().noneMatch(it -> it.getName().equals("Sec-WebSocket-Protocol"))) {
      headers.add(new HttpHeader("Sec-WebSocket-Protocol", wsProtocolFactory.getName()));
    }
    WebSocketConnection webSocketConnection = new WebSocketConnection(webSocketFactory, serverUrl, headers);
    webSocketConnection.open();
    return webSocketConnection;
  }

  private void initWsProtocol(WebSocketConnection webSocket) {
    wsProtocol = wsProtocolFactory.create(webSocket, listener);
    wsProtocol.connectionInit();
  }

  private void stopWsProtocolIfNoMoreSubscriptions() {
    // TODO do this after a configurable idle timeout
    if (activeSubscriptions.isEmpty() && wsProtocol != null) {
      wsProtocol.close();
      wsProtocol = null;
    }
  }

  private void disposeSubscription(String id) {
    SubscriptionInfo subscriptionInfo = activeSubscriptions.get(id);
    if (subscriptionInfo == null) return;
    activeSubscriptions.remove(id);
    subscriptionInfo.disposable.dispose();
    stopWsProtocolIfNoMoreSubscriptions();
  }

  private final WsProtocol.Listener listener = new WsProtocol.Listener() {
    @Override public void operationResponse(String id, Map<String, Object> payload) {
      SubscriptionInfo subscriptionInfo = activeSubscriptions.get(id);
      if (subscriptionInfo == null) return;
      ApolloRequest<?> request = subscriptionInfo.request;
      CustomScalarAdapters customScalarAdapters = request.getExecutionContext().get(CustomScalarAdapters.Key);
      JsonReader jsonReader = new MapJsonReader(payload);
      ApolloResponse apolloResponse = Operations.parseJsonResponse(request.getOperation(), jsonReader, customScalarAdapters)
          .newBuilder()
          .requestUuid(request.getRequestUuid())
          .build();
      //noinspection unchecked
      subscriptionInfo.callback.onResponse(apolloResponse);
    }

    @Override public void operationError(String id, Map<String, Object> payload) {
      SubscriptionInfo subscriptionInfo = activeSubscriptions.get(id);
      if (subscriptionInfo == null) return;
      subscriptionInfo.callback.onFailure(new SubscriptionOperationException(subscriptionInfo.request.getOperation().name(), payload));
      disposeSubscription(id);
    }

    @Override public void operationComplete(String id) {
      disposeSubscription(id);
    }

    @Override public void generalError(Map<String, Object> payload) {
      // The server sends an error without an operation id. This happens when sending an unknown message type
      // to https://apollo-fullstack-tutorial.herokuapp.com/ for an example. In that case, this error is not fatal
      // and the server will continue honouring other subscriptions, so we just filter the error out and log it.
      System.out.println("Received general error: " + payload);
    }

    @Override public void networkError(Throwable cause) {
      ApolloNetworkException networkException = new ApolloNetworkException("Network error", cause);
      List<SubscriptionInfo> activeSubscriptionList = new ArrayList<>(activeSubscriptions.values());
      activeSubscriptions.clear();
      for (SubscriptionInfo subscriptionInfo : activeSubscriptionList) {
        subscriptionInfo.callback.onFailure(networkException);
        subscriptionInfo.disposable.dispose();
      }
      stopWsProtocolIfNoMoreSubscriptions();
    }
  };


  private static class SubscriptionInfo {
    private ApolloRequest<?> request;
    private ApolloCallback<?> callback;
    private ApolloDisposable disposable;

    public SubscriptionInfo(@NotNull ApolloRequest<?> request, @NotNull ApolloCallback<?> callback, ApolloDisposable disposable) {
      this.request = request;
      this.callback = callback;
      this.disposable = disposable;
    }
  }
}
