package com.apollographql.apollo3.runtime.java.internal.ws;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Operation;
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
  private CustomScalarAdapters customScalarAdapters;

  private WebSocketConnection webSocketConnection;
  private Map<UUID, List<ApolloCallback<?>>> activeSubscriptions = new HashMap<>();

  public WebSocketNetworkTransport(WebSocket.Factory webSocketFactory, String serverUrl, CustomScalarAdapters customScalarAdapters) {
    this.customScalarAdapters = customScalarAdapters;
    webSocketConnection = new WebSocketConnection(webSocketFactory, serverUrl, null /*TODO headers!*/);
  }

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
    if (!webSocketConnection.isOpen()) {
      boolean openSuccess = webSocketConnection.open();
//      connectionInit();
    }
  }


}


