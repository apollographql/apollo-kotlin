package com.apollographql.apollo.runtime.java.network.ws.protocol;

import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.ImmutableMapBuilder;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer;
import com.apollographql.apollo.runtime.java.network.ws.WebSocketConnection;

import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A {@link WsProtocol} for <a href="https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md">Apollo's GraphQL
 * over WebSocket Protocol</a>.
 * <p>
 * Note: This protocol is no longer actively maintained, and {@link GraphQLWsProtocol} should be favored instead.
 */
public class ApolloWsProtocol extends WsProtocol {
  private Supplier<Map<String, Object>> connectionPayload;
  private WsFrameType frameType;

  public ApolloWsProtocol(
      WebSocketConnection webSocketConnection,
      Listener listener,
      Supplier<Map<String, Object>> connectionPayload,
      WsFrameType frameType
  ) {
    super(webSocketConnection, listener);
    this.connectionPayload = connectionPayload;
    this.frameType = frameType;
  }

  @Override
  public void connectionInit() {
    Map<String, Object> message = new ImmutableMapBuilder<String, Object>().put("type", "connection_init").build();
    Map<String, Object> payload = connectionPayload.get();
    if (payload != null) {
      message.put("payload", payload);
    }
    sendMessageMap(message, frameType);

    Map<String, Object> map = receiveMessageMap(-1L);
    if (map == null) {
      listener.networkError(new IOException("Connection closed while waiting for connection_ack"));
    } else {
      Object type = map.get("type");
      if ("connection_error".equals(type)) {
        listener.networkError(new IOException("connection_error received " + map));
      } else if (!"connection_ack".equals(type)) {
        System.out.println("unknown graphql-ws message while waiting for connection_ack: " + type);
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
    }
  }

  @Override
  public <D extends Operation.Data> void startOperation(ApolloRequest<D> request) {
    sendMessageMap(
        new ImmutableMapBuilder<String, Object>()
            .put("type", "start")
            .put("id", request.getRequestUuid().toString())
            .put("payload", DefaultHttpRequestComposer.Companion.composePayload(request))
            .build(),
        frameType
    );
  }

  @Override
  public <D extends Operation.Data> void stopOperation(ApolloRequest<D> request) {
    sendMessageMap(
        new ImmutableMapBuilder<String, Object>()
            .put("type", "stop")
            .put("id", request.getRequestUuid().toString())
            .build(),
        frameType
    );
  }

  public static class Factory implements WsProtocol.Factory {
    private Supplier<Map<String, Object>> connectionPayload;
    private WsFrameType frameType;

    public Factory() {
      this(() -> null);
    }

    public Factory(Supplier<Map<String, Object>> connectionPayload) {
      this(connectionPayload, WsFrameType.Text);
    }


    public Factory(Supplier<Map<String, Object>> connectionPayload, WsFrameType frameType) {
      this.connectionPayload = connectionPayload;
      this.frameType = frameType;
    }

    @Override public String getName() {
      return "apollo-ws";
    }

    @Override public WsProtocol create(WebSocketConnection webSocketConnection, Listener listener) {
      return new ApolloWsProtocol(webSocketConnection, listener, connectionPayload, frameType);
    }
  }
}
