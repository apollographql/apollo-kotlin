package com.apollographql.apollo3.runtime.java.internal.ws;

import com.apollographql.apollo3.api.ApolloRequest;
import com.apollographql.apollo3.api.Operation;
import com.apollographql.apollo3.api.http.DefaultHttpRequestComposer;

import java.util.Map;
import java.util.function.Supplier;

import static com.apollographql.apollo3.runtime.java.internal.MapUtils.entry;
import static com.apollographql.apollo3.runtime.java.internal.MapUtils.mapOf;

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

  @Override void connectionInit() {
    Map<String, Object> message = mapOf(entry("type", "connection_init"));
    Map<String, Object> payload = connectionPayload.get();
    if (payload != null) {
      message.put("payload", payload);
    }
    sendMessageMap(message, frameType);

    Map<String, Object> map = receiveMessageMap();
    Object type = map.get("type");
    if ("connection_error".equals(type)) {
      // TODO
    } else if (!"connection_ack".equals(type)) {
      System.out.println("unknown graphql-ws message while waiting for connection_ack: " + type);
    }
  }

  @Override void handleServerMessage(Map<String, Object> messageMap) {

  }

  @Override <D extends Operation.Data> void startOperation(ApolloRequest<D> request) {
    sendMessageMap(
        mapOf(
            entry("type", "start"),
            entry("id", request.getRequestUuid().toString()),
            entry("payload", DefaultHttpRequestComposer.Companion.composePayload(request))
        ),
        frameType
    );
  }

  @Override <D extends Operation.Data> void stopOperation(ApolloRequest<D> request) {

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
