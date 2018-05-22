package com.apollographql.apollo.subscription;

import com.apollographql.apollo.internal.json.BufferedSourceJsonReader;
import com.apollographql.apollo.internal.json.JsonReader;
import com.apollographql.apollo.internal.json.ResponseJsonStreamReader;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import okio.Buffer;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static java.util.Collections.unmodifiableMap;

@SuppressWarnings("WeakerAccess")
public abstract class OperationServerMessage {
  static final String JSON_KEY_ID = "id";
  static final String JSON_KEY_TYPE = "type";
  static final String JSON_KEY_PAYLOAD = "payload";

  OperationServerMessage() {
  }

  @NotNull public static OperationServerMessage fromJsonString(@NotNull String json) {
    checkNotNull(json, "json == null");
    try {
      Buffer buffer = new Buffer();
      buffer.writeUtf8(json);
      return OperationServerMessage.readFromJson(new BufferedSourceJsonReader(buffer));
    } catch (Exception e) {
      return new Unsupported(json);
    }
  }

  private static OperationServerMessage readFromJson(@NotNull JsonReader reader) throws IOException {
    checkNotNull(reader, "reader == null");

    ResponseJsonStreamReader responseJsonStreamReader = new ResponseJsonStreamReader(reader);
    Map<String, Object> messageData = responseJsonStreamReader.toMap();
    String id = (String) messageData.get(JSON_KEY_ID);
    String type = (String) messageData.get(JSON_KEY_TYPE);
    switch (type) {
      case ConnectionError.TYPE:
        return new ConnectionError(messagePayload(messageData));

      case ConnectionAcknowledge.TYPE:
        return new ConnectionAcknowledge();

      case Data.TYPE:
        return new Data(id, messagePayload(messageData));

      case Error.TYPE:
        return new Error(id, messagePayload(messageData));

      case Complete.TYPE:
        return new Complete(id);

      case ConnectionKeepAlive.TYPE:
        return new ConnectionKeepAlive();

      default:
        throw new IOException("Unsupported message");
    }
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> messagePayload(Map<String, Object> messageData) {
    Map<String, Object> messagePayload = (Map<String, Object>) messageData.get(JSON_KEY_PAYLOAD);
    return messagePayload == null ? Collections.<String, Object>emptyMap() : unmodifiableMap(messagePayload);
  }

  public static final class ConnectionError extends OperationServerMessage {
    public static final String TYPE = "connection_error";
    public final Map<String, Object> payload;

    public ConnectionError(Map<String, Object> payload) {
      this.payload = payload;
    }
  }

  public static final class ConnectionAcknowledge extends OperationServerMessage {
    public static final String TYPE = "connection_ack";

    public ConnectionAcknowledge() {
    }
  }

  public static final class Data extends OperationServerMessage {
    public static final String TYPE = "data";
    public final String id;
    public final Map<String, Object> payload;

    public Data(String id, Map<String, Object> payload) {
      this.id = id;
      this.payload = payload;
    }
  }

  public static final class Error extends OperationServerMessage {
    public static final String TYPE = "error";
    public final String id;
    public final Map<String, Object> payload;

    public Error(String id, Map<String, Object> payload) {
      this.id = id;
      this.payload = payload;
    }
  }

  public static final class Complete extends OperationServerMessage {
    public static final String TYPE = "complete";
    public final String id;

    public Complete(String id) {
      this.id = id;
    }
  }

  public static final class ConnectionKeepAlive extends OperationServerMessage {
    public static final String TYPE = "ka";
  }

  public static final class Unsupported extends OperationServerMessage {
    public final String rawMessage;

    public Unsupported(String rawMessage) {
      this.rawMessage = rawMessage;
    }
  }
}
