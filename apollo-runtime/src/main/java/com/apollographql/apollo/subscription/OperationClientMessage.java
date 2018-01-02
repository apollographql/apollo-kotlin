package com.apollographql.apollo.subscription;

import com.apollographql.apollo.api.Subscription;
import com.apollographql.apollo.internal.json.InputFieldJsonWriter;
import com.apollographql.apollo.internal.json.JsonWriter;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import java.io.IOException;

import javax.annotation.Nonnull;

import okio.Buffer;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

@SuppressWarnings("WeakerAccess")
public abstract class OperationClientMessage {
  static final String JSON_KEY_ID = "id";
  static final String JSON_KEY_TYPE = "type";
  static final String JSON_KEY_PAYLOAD = "payload";

  OperationClientMessage() {
  }

  public String toJsonString() {
    try {
      Buffer buffer = new Buffer();
      JsonWriter writer = JsonWriter.of(buffer);
      writer.beginObject();
      writeToJson(writer);
      writer.endObject();
      writer.close();
      return buffer.readUtf8();
    } catch (IOException e) {
      throw new RuntimeException("Failed to serialize to json", e);
    }
  }

  public abstract void writeToJson(@Nonnull JsonWriter writer) throws IOException;

  public static final class Init extends OperationClientMessage {
    private static final String TYPE = "connection_init";

    @Override public void writeToJson(@Nonnull JsonWriter writer) throws IOException {
      checkNotNull(writer, "writer == null");
      writer.name(JSON_KEY_TYPE).value(TYPE);
    }
  }

  public static final class Start extends OperationClientMessage {
    private static final String TYPE = "start";
    private static final String JSON_KEY_QUERY = "query";
    private static final String JSON_KEY_VARIABLES = "variables";
    private static final String JSON_KEY_OPERATION_NAME = "operationName";
    private final ScalarTypeAdapters scalarTypeAdapters;

    public final String subscriptionId;
    public final Subscription<?, ?, ?> subscription;

    public Start(@Nonnull String subscriptionId, @Nonnull Subscription<?, ?, ?> subscription,
        @Nonnull ScalarTypeAdapters scalarTypeAdapters) {
      this.subscriptionId = checkNotNull(subscriptionId, "subscriptionId == null");
      this.subscription = checkNotNull(subscription, "subscription == null");
      this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
    }

    @Override public void writeToJson(@Nonnull JsonWriter writer) throws IOException {
      checkNotNull(writer, "writer == null");
      writer.name(JSON_KEY_ID).value(subscriptionId);
      writer.name(JSON_KEY_TYPE).value(TYPE);
      writer.name(JSON_KEY_PAYLOAD).beginObject();
      writer.name(JSON_KEY_QUERY).value(subscription.queryDocument().replaceAll("\\n", ""));
      writer.name(JSON_KEY_VARIABLES).beginObject();
      subscription.variables().marshaller().marshal(new InputFieldJsonWriter(writer, scalarTypeAdapters));
      writer.endObject();
      writer.name(JSON_KEY_OPERATION_NAME).value(subscription.name().name());
      writer.endObject();
    }
  }

  public static final class Stop extends OperationClientMessage {
    private static final String TYPE = "stop";

    public final String subscriptionId;

    public Stop(@Nonnull String subscriptionId) {
      this.subscriptionId = checkNotNull(subscriptionId, "subscriptionId == null");
    }

    @Override public void writeToJson(@Nonnull JsonWriter writer) throws IOException {
      checkNotNull(writer, "writer == null");
      writer.name(JSON_KEY_ID).value(subscriptionId);
      writer.name(JSON_KEY_TYPE).value(TYPE);
    }
  }

  public static final class Terminate extends OperationClientMessage {
    private static final String TYPE = "connection_terminate";

    @Override public void writeToJson(@Nonnull JsonWriter writer) throws IOException {
      checkNotNull(writer, "writer == null");
      writer.name(JSON_KEY_TYPE).value(TYPE);
    }
  }
}
