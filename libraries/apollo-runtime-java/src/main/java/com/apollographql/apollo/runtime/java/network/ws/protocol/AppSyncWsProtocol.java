package com.apollographql.apollo.runtime.java.network.ws.protocol;

import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.ImmutableMapBuilder;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer;
import com.apollographql.apollo.runtime.java.network.ws.WebSocketConnection;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Map;


/**
 * A {@link WsProtocol} for the <a href="https://docs.aws.amazon.com/appsync/latest/devguide/real-time-websocket-client.html">AWS AppSync
 * Protocol</a>.
 */
public class AppSyncWsProtocol extends WsProtocol {
  private Map<String, Object> authorization;
  private long connectionAcknowledgeTimeoutMs;

  public AppSyncWsProtocol(
      WebSocketConnection webSocketConnection,
      Listener listener,
      Map<String, Object> authorization,
      long connectionAcknowledgeTimeoutMs
  ) {
    super(webSocketConnection, listener);
    this.authorization = authorization;
    this.connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs;
  }

  @Override
  public void connectionInit() {
    Map<String, Object> message = new ImmutableMapBuilder<String, Object>().put("type", "connection_init").build();
    sendMessageMapText(message);

    Map<String, Object> map = receiveMessageMap(connectionAcknowledgeTimeoutMs);
    if (map == null) {
      listener.networkError(new IOException("Connection closed or timeout while waiting for connection_ack"));
    } else {
      Object type = map.get("type");
      if ("connection_error".equals(type)) {
        listener.networkError(new IOException("connection_error received " + map));
      } else if (!"connection_ack".equals(type)) {
        System.out.println("unknown graphql-transport-ws message while waiting for connection_ack: " + type);
      }
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void handleServerMessage(Map<String, Object> messageMap) {
    String type = (String) messageMap.get("type");
    switch (type) {
      case "data":
        listener.operationResponse((String) messageMap.get("id"), (Map<String, Object>) messageMap.get("payload"));
        break;
      case "error":
        String id = (String) messageMap.get("id");
        if (id != null) {
          listener.operationError(id, (Map<String, Object>) messageMap.get("payload"));
        } else {
          listener.generalError((Map<String, Object>) messageMap.get("payload"));
        }
        break;
      case "complete":
        listener.operationComplete((String) messageMap.get("id"));
        break;
      case "ka":
        // keep alive: nothing to do
        break;
    }
  }

  @Override
  public <D extends Operation.Data> void startOperation(ApolloRequest<D> request) {
    String data = toJsonString(DefaultHttpRequestComposer.Companion.composePayload(request));
    sendMessageMapText(
        new ImmutableMapBuilder<String, Object>()
            .put("type", "start")
            .put("id", request.getRequestUuid().toString())
            .put("payload", new ImmutableMapBuilder<String, Object>()
                .put("data", data)
                .put("extensions", new ImmutableMapBuilder<String, Object>()
                    .put("authorization", authorization)
                    .build()
                )
                .build()
            )
            .build()
    );
  }

  @Override
  public <D extends Operation.Data> void stopOperation(ApolloRequest<D> request) {
    sendMessageMapText(
        new ImmutableMapBuilder<String, Object>()
            .put("type", "stop")
            .put("id", request.getRequestUuid().toString())
            .build()
    );
  }

  public static class Factory implements WsProtocol.Factory {
    private Map<String, Object> authorization;
    private long connectionAcknowledgeTimeoutMs;

    public Factory(Map<String, Object> authorization) {
      this(authorization, 10000);
    }

    public Factory(Map<String, Object> authorization, long connectionAcknowledgeTimeoutMs) {
      this.authorization = authorization;
      this.connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs;
    }

    @Override public String getName() {
      return "graphql-ws";
    }

    @Override public WsProtocol create(WebSocketConnection webSocketConnection, Listener listener) {
      return new AppSyncWsProtocol(webSocketConnection, listener, authorization, connectionAcknowledgeTimeoutMs);
    }
  }

  /**
   * Helper method that builds the final URL. It will append the authorization and payload arguments as query parameters. This method can be
   * used for both the HTTP URL as well as the WebSocket URL.
   * <p>
   * Example:
   * <pre>
   *     Map<String, Object> authorization = new HashMap<String, Object>();
   *     authorization.put("host", "example1234567890000.appsync-api.us-east-1.amazonaws.com");
   *     // This example uses an API key. See the AppSync documentation for information on what to pass
   *     authorization.put("x-api-key", "da2-12345678901234567890123456");
   *     String url = buildUrl(
   *        "https://example1234567890000.appsync-realtime-api.us-east-1.amazonaws.com/graphql",
   *        authorization,
   *        new HashMap<String, Object>()
   *     );
   * </pre>
   *
   * @param baseUrl The base web socket URL.
   * @param authorization The authorization as per the AppSync documentation.
   * @param payload An optional payload - can be empty but must not be null
   */
  @NotNull
  public static String buildUrl(
      @NotNull String baseUrl,
      @NotNull Map<String, Object> authorization,
      @NotNull Map<String, Object> payload
  ) {
    return DefaultHttpRequestComposer.Companion.appendQueryParameters(baseUrl, new ImmutableMapBuilder<String, String>()
        .put("header", toJsonByteString(authorization).base64())
        .put("payload", toJsonByteString(payload).base64())
        .build()
    );
  }
}
