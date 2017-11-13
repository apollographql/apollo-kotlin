package com.apollographql.apollo.response;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.internal.json.JsonWriter;
import com.apollographql.apollo.internal.response.RealResponseWriter;
import com.apollographql.apollo.internal.response.ResolveDelegate;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public class OperationJsonWriter {
  private final Operation.Data operationData;
  private final ScalarTypeAdapters scalarTypeAdapters;

  public OperationJsonWriter(Operation.Data operationData, ScalarTypeAdapters scalarTypeAdapters) {
    this.operationData = operationData;
    this.scalarTypeAdapters = scalarTypeAdapters;
  }

  public void write(@Nonnull JsonWriter jsonWriter) throws IOException {
    checkNotNull(jsonWriter, "jsonWriter == null");

    RealResponseWriter realResponseWriter = new RealResponseWriter(Operation.EMPTY_VARIABLES, scalarTypeAdapters);
    operationData.marshaller().marshal(realResponseWriter);

    jsonWriter.beginObject();
    jsonWriter.name("data");
    jsonWriter.beginObject();

    try {
      realResponseWriter.resolveFields(new JsonResponseResolver(jsonWriter));
    } catch (RuntimeException e) {
      if (e.getCause() instanceof IOException) {
        throw (IOException) e.getCause();
      } else {
        throw e;
      }
    }

    jsonWriter.endObject();
    jsonWriter.endObject();
  }

  private static final class JsonResponseResolver implements ResolveDelegate<Map<String, Object>> {
    private final JsonWriter jsonWriter;

    JsonResponseResolver(JsonWriter jsonWriter) {
      this.jsonWriter = jsonWriter;
    }

    @Override public void willResolveRootQuery(Operation operation) {
    }

    @Override public void willResolve(ResponseField field, Operation.Variables variables) {
      try {
        jsonWriter.name(field.responseName());
        if (field.type() == ResponseField.Type.LIST) {
          jsonWriter.beginArray();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override public void didResolve(ResponseField field, Operation.Variables variables) {
    }

    @Override public void didResolveScalar(Object value) {
      try {
        if (value instanceof Number) {
          jsonWriter.value((Number) value);
        } else if (value instanceof Boolean) {
          jsonWriter.value((boolean) value);
        } else {
          jsonWriter.value(value.toString());
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override public void willResolveObject(ResponseField field, Optional<Map<String, Object>> objectSource) {
      try {
        jsonWriter.beginObject();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override public void didResolveObject(ResponseField field, Optional<Map<String, Object>> objectSource) {
      try {
        jsonWriter.endObject();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override public void didResolveList(List array) {
      try {
        jsonWriter.endArray();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    @Override public void willResolveElement(int atIndex) {
    }

    @Override public void didResolveElement(int atIndex) {
    }

    @Override public void didResolveNull() {
      try {
        jsonWriter.nullValue();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
