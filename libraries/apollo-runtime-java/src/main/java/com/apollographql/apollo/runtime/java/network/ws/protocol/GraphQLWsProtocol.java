package com.apollographql.apollo.runtime.java.network.ws.protocol;

import com.apollographql.apollo.api.ApolloRequest;
import com.apollographql.apollo.api.ImmutableMapBuilder;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.http.DefaultHttpRequestComposer;
import com.apollographql.apollo.runtime.java.network.ws.WebSocketConnection;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * An {@link WsProtocol} for <a href="https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md">the GraphQL over WebSocket
 * Protocol</a>. It can carry queries in addition to subscriptions over the websocket.
 */
public class GraphQLWsProtocol extends WsProtocol {
  private Supplier<Map<String, Object>> connectionPayload;
  private WsFrameType frameType;
  private Map<String, Object> pingPayload;
  private Map<String, Object> pongPayload;
  private long connectionAcknowledgeTimeoutMs;
  private long pingIntervalMillis;

  private ScheduledExecutorService pingExecutor = Executors.newSingleThreadScheduledExecutor();

  public GraphQLWsProtocol(
      WebSocketConnection webSocketConnection,
      Listener listener,
      Supplier<Map<String, Object>> connectionPayload,
      WsFrameType frameType,
      Map<String, Object> pingPayload,
      Map<String, Object> pongPayload,
      long connectionAcknowledgeTimeoutMs,
      long pingIntervalMillis
  ) {
    super(webSocketConnection, listener);
    this.connectionPayload = connectionPayload;
    this.frameType = frameType;
    this.pingPayload = pingPayload;
    this.pongPayload = pongPayload;
    this.connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs;
    this.pingIntervalMillis = pingIntervalMillis;
  }

  @Override
  public void connectionInit() {
    Map<String, Object> message = new ImmutableMapBuilder<String, Object>().put("type", "connection_init").build();
    Map<String, Object> payload = connectionPayload.get();
    if (payload != null) {
      message.put("payload", payload);
    }
    sendMessageMap(message, frameType);

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
      case "next":
        listener.operationResponse((String) messageMap.get("id"), (Map<String, Object>) messageMap.get("payload"));
        break;
      case "error":
        listener.operationError((String) messageMap.get("id"), (Map<String, Object>) messageMap.get("payload"));
        break;
      case "complete":
        listener.operationComplete((String) messageMap.get("id"));
        break;
      case "ping":
        Map<String, Object> map = new ImmutableMapBuilder<String, Object>().put("type", "pong").build();
        if (pongPayload != null) {
          map.put("payload", pongPayload);
        }
        sendMessageMap(map, frameType);
        break;
      case "pong":
        // Nothing to do, the server acknowledged one of our pings
        break;
      default:
        // Unknown message
        break;
    }
  }

  @Override
  public <D extends Operation.Data> void startOperation(ApolloRequest<D> request) {
    sendMessageMap(
        new ImmutableMapBuilder<String, Object>()
            .put("type", "subscribe")
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
            .put("type", "complete")
            .put("id", request.getRequestUuid().toString())
            .build(),
        frameType
    );
  }

  @Override
  public void run() {
    if (pingIntervalMillis > 0) {
      pingExecutor.scheduleWithFixedDelay(() -> {
        Map<String, Object> map = new ImmutableMapBuilder<String, Object>()
            .put("type", "ping")
            .build();
        if (pingPayload != null) {
          map.put("payload", pingPayload);
        }
        sendMessageMap(map, frameType);
      }, pingIntervalMillis, pingIntervalMillis, TimeUnit.MILLISECONDS);
    }

    super.run();
  }

  public static class Factory implements WsProtocol.Factory {
    private Supplier<Map<String, Object>> connectionPayload;
    private WsFrameType frameType;
    private Map<String, Object> pingPayload;
    private Map<String, Object> pongPayload;
    private long connectionAcknowledgeTimeoutMs;
    private long pingIntervalMillis;

    public Factory() {
      this(() -> null, WsFrameType.Text, null, null, 60000, -1);
    }

    public Factory(
        Supplier<Map<String, Object>> connectionPayload,
        WsFrameType frameType,
        Map<String, Object> pingPayload,
        Map<String, Object> pongPayload,
        long connectionAcknowledgeTimeoutMs,
        long pingIntervalMillis
    ) {
      this.connectionPayload = connectionPayload;
      this.frameType = frameType;
      this.pingPayload = pingPayload;
      this.pongPayload = pongPayload;
      this.connectionAcknowledgeTimeoutMs = connectionAcknowledgeTimeoutMs;
      this.pingIntervalMillis = pingIntervalMillis;
    }

    @Override public String getName() {
      return "graphql-transport-ws";
    }

    @Override public WsProtocol create(
        WebSocketConnection webSocketConnection,
        Listener listener
    ) {
      return new GraphQLWsProtocol(
          webSocketConnection,
          listener,
          connectionPayload,
          frameType,
          pingPayload,
          pongPayload,
          connectionAcknowledgeTimeoutMs,
          pingIntervalMillis
      );
    }
  }

}
