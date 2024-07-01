package com.apollographql.apollo.runtime.java.network.ws;

import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.ApolloResponse;
import com.apollographql.apollo.api.CustomScalarAdapters;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.Operations;
import com.apollographql.apollo.api.http.HttpHeader;
import com.apollographql.apollo.api.json.JsonReader;
import com.apollographql.apollo.api.json.MapJsonReader;
import com.apollographql.apollo.exception.ApolloNetworkException;
import com.apollographql.apollo.exception.SubscriptionOperationException;
import com.apollographql.apollo.runtime.java.ApolloCallback;
import com.apollographql.apollo.runtime.java.ApolloDisposable;
import com.apollographql.apollo.runtime.java.network.NetworkTransport;
import com.apollographql.apollo.runtime.java.network.ws.protocol.WsProtocol;
import okhttp3.WebSocket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public class WebSocketNetworkTransport implements NetworkTransport {
  private WebSocket.Factory webSocketFactory;
  private String serverUrl;
  private WsProtocol.Factory wsProtocolFactory;
  private List<HttpHeader> headers;
  private ReopenWhen reopenWhen;
  private Executor executor;
  private long idleTimeoutMillis;

  private Map<String, SubscriptionInfo> activeSubscriptions = Collections.synchronizedMap(new HashMap<>());
  private AtomicReference<WsProtocol> wsProtocol = new AtomicReference<>();

  public WebSocketNetworkTransport(
      WebSocket.Factory webSocketFactory,
      WsProtocol.Factory wsProtocolFactory,
      String serverUrl,
      List<HttpHeader> headers,
      ReopenWhen reopenWhen,
      Executor executor,
      long idleTimeoutMillis
  ) {
    this.webSocketFactory = webSocketFactory;
    this.serverUrl = serverUrl;
    this.wsProtocolFactory = wsProtocolFactory;
    this.headers = headers;
    this.reopenWhen = reopenWhen;
    this.executor = executor;
    this.idleTimeoutMillis = idleTimeoutMillis;
  }

  @Override public <D extends Operation.Data> void execute(@NotNull ApolloRequest<D> request, @NotNull ApolloCallback<D> callback, ApolloDisposable disposable) {
    SubscriptionInfo subscriptionInfo = new SubscriptionInfo(request, callback, disposable);
    String id = request.getRequestUuid().toString();
    activeSubscriptions.put(id, subscriptionInfo);
    disposable.addListener(() -> {
      if (activeSubscriptions.containsKey(id)) {
        // Caller has cancelled the subscription by calling ApolloDisposable.dispose()
        WsProtocol curWsProtocol = wsProtocol.get();
        if (curWsProtocol != null) curWsProtocol.stopOperation(request);
        disposeSubscription(id);
      }
    });
    WsProtocol runningWsProtocol = ensureWsProtocolRunning();
    if (runningWsProtocol != null) runningWsProtocol.startOperation(request);
  }

  @Override public void dispose() {
  }

  @Nullable private WsProtocol ensureWsProtocolRunning() {
    synchronized (this) {
      WsProtocol curWsProtocol = wsProtocol.get();
      if (curWsProtocol == null) {
        WebSocketConnection webSocket;
        try {
          webSocket = openWebSocket();
        } catch (Throwable e) {
          listener.networkError(e);
          return null;
        }
        WsProtocol newWsProtocol = wsProtocolFactory.create(webSocket, listener);
        wsProtocol.set(newWsProtocol);
        newWsProtocol.connectionInit();
        executor.execute(newWsProtocol::run);
        return newWsProtocol;
      } else {
        return curWsProtocol;
      }
    }
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

  private void scheduleStopWsProtocolIfNoMoreSubscriptions() {
    executor.execute(() -> {
      try {
        Thread.sleep(idleTimeoutMillis);
      } catch (InterruptedException ignored) {
      }
      stopWsProtocolIfNoMoreSubscriptions();
    });
  }

  private void stopWsProtocolIfNoMoreSubscriptions() {
    WsProtocol curWsProtocol = wsProtocol.get();
    if (activeSubscriptions.isEmpty() && curWsProtocol != null) {
      curWsProtocol.close();
      wsProtocol.set(null);
    }
  }

  private void disposeSubscription(String id) {
    SubscriptionInfo subscriptionInfo = activeSubscriptions.get(id);
    if (subscriptionInfo == null) return;
    activeSubscriptions.remove(id);
    subscriptionInfo.disposable.dispose();
    scheduleStopWsProtocolIfNoMoreSubscriptions();
  }

  private final WsProtocol.Listener listener = new WsProtocol.Listener() {
    @Override public void operationResponse(String id, Map<String, Object> payload) {
      SubscriptionInfo subscriptionInfo = activeSubscriptions.get(id);
      if (subscriptionInfo == null) return;
      ApolloRequest<?> request = subscriptionInfo.request;
      CustomScalarAdapters customScalarAdapters = request.getExecutionContext().get(CustomScalarAdapters.Key);
      JsonReader jsonReader = new MapJsonReader(payload);
      //noinspection rawtypes
      ApolloResponse apolloResponse = Operations.toApolloResponse(jsonReader, request.getOperation(), request.getRequestUuid(), customScalarAdapters, null);
      //noinspection unchecked
      subscriptionInfo.callback.onResponse(apolloResponse);
    }

    @Override public void operationError(String id, Map<String, Object> payload) {
      SubscriptionInfo subscriptionInfo = activeSubscriptions.get(id);
      if (subscriptionInfo == null) return;
      ApolloRequest<?> request = subscriptionInfo.request;
      //noinspection unchecked,rawtypes
      subscriptionInfo.callback.onResponse(new ApolloResponse.Builder(request.getOperation(), request.getRequestUuid())
          .exception(new SubscriptionOperationException(request.getOperation().name(), payload))
          .build());
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

    private int reopenAttempt = -1;


    @Override public void networkError(Throwable cause) {
      wsProtocol.set(null);
      reopenAttempt++;
      if (reopenWhen.reopenWhen(cause, reopenAttempt)) {
        WsProtocol runningWsProtocol = ensureWsProtocolRunning();
        if (runningWsProtocol != null) {
          reopenAttempt = -1;
          // Re-subscribe to all active subscriptions
          List<SubscriptionInfo> activeSubscriptionList = new ArrayList<>(activeSubscriptions.values());
          for (SubscriptionInfo subscriptionInfo : activeSubscriptionList) {
            runningWsProtocol.startOperation(subscriptionInfo.request);
          }
        }
      } else {
        reopenAttempt = -1;
        ApolloNetworkException networkException = new ApolloNetworkException("Network error", cause);
        List<SubscriptionInfo> activeSubscriptionList = new ArrayList<>(activeSubscriptions.values());
        activeSubscriptions.clear();
        for (SubscriptionInfo subscriptionInfo : activeSubscriptionList) {
          //noinspection unchecked,rawtypes
          subscriptionInfo.callback.onResponse(new ApolloResponse.Builder(subscriptionInfo.request.getOperation(), subscriptionInfo.request.getRequestUuid())
              .exception(networkException)
              .build());
          subscriptionInfo.disposable.dispose();
        }
        stopWsProtocolIfNoMoreSubscriptions();
      }
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

  public interface ReopenWhen {
    boolean reopenWhen(Throwable throwable, long attempt);
  }
}
