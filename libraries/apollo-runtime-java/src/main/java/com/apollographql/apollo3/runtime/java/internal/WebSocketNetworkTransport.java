package com.apollographql.apollo3.runtime.java.internal;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.CustomScalarAdapters;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.http.HttpRequestComposer;
import com.apollographql.apollo3.runtime.java.ApolloCallback;
import com.apollographql.apollo3.runtime.java.interceptor.ApolloDisposable;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;

public class WebSocketNetworkTransport {
  private WebSocket.Factory webSocketFactory;
  private HttpRequestComposer requestComposer;
  private CustomScalarAdapters customScalarAdapters;

  public WebSocketNetworkTransport(WebSocket.Factory webSocketFactory, HttpRequestComposer httpRequestComposer, CustomScalarAdapters customScalarAdapters) {
    this.webSocketFactory = webSocketFactory;
    this.requestComposer = httpRequestComposer;
    this.customScalarAdapters = customScalarAdapters;
  }

  public <D extends Operation.Data> void execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback, ApolloDisposable disposable) {

  }
}
