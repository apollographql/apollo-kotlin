package com.apollographql.apollo.internal.json;

import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.CustomTypeValue;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import java.io.IOException;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.json.Utils.writeToJson;

public class InputFieldJsonWriter implements InputFieldWriter {
  private final JsonWriter jsonWriter;
  private final ScalarTypeAdapters scalarTypeAdapters;

  public InputFieldJsonWriter(JsonWriter jsonWriter, ScalarTypeAdapters scalarTypeAdapters) {
    this.jsonWriter = jsonWriter;
    this.scalarTypeAdapters = scalarTypeAdapters;
  }

  @Override public void writeString(@NotNull String fieldName, String value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeInt(@NotNull String fieldName, Integer value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeLong(@NotNull String fieldName, Long value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeDouble(@NotNull String fieldName, Double value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeNumber(@NotNull String fieldName, Number value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeBoolean(@NotNull String fieldName, Boolean value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @SuppressWarnings("unchecked")
  @Override public void writeCustom(@NotNull String fieldName, ScalarType scalarType, Object value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      CustomTypeAdapter customTypeAdapter = scalarTypeAdapters.adapterFor(scalarType);
      CustomTypeValue customTypeValue = customTypeAdapter.encode(value);
      if (customTypeValue instanceof CustomTypeValue.GraphQLString) {
        writeString(fieldName, ((CustomTypeValue.GraphQLString) customTypeValue).value);
      } else if (customTypeValue instanceof CustomTypeValue.GraphQLBoolean) {
        writeBoolean(fieldName, ((CustomTypeValue.GraphQLBoolean) customTypeValue).value);
      } else if (customTypeValue instanceof CustomTypeValue.GraphQLNumber) {
        writeNumber(fieldName, ((CustomTypeValue.GraphQLNumber) customTypeValue).value);
      } else if (customTypeValue instanceof CustomTypeValue.GraphQLJsonString) {
        writeString(fieldName, ((CustomTypeValue.GraphQLJsonString) customTypeValue).value);
      } else if (customTypeValue instanceof CustomTypeValue.GraphQLJson) {
        writeMap(fieldName, ((CustomTypeValue.GraphQLJson) customTypeValue).value);
      } else {
        throw new IllegalArgumentException("Unsupported custom value type: " + customTypeValue);
      }
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeObject(@NotNull String fieldName, InputFieldMarshaller marshaller) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (marshaller != null) {
      jsonWriter.name(fieldName).beginObject();
      marshaller.marshal(this);
      jsonWriter.endObject();
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeList(@NotNull String fieldName, ListWriter listWriter) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (listWriter != null) {
      jsonWriter.name(fieldName).beginArray();
      listWriter.write(new JsonListItemWriter(jsonWriter, scalarTypeAdapters));
      jsonWriter.endArray();
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeMap(@NotNull String fieldName, Map<String, Object> value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value == null) {
      jsonWriter.name(fieldName).nullValue();
    } else {
      jsonWriter.name(fieldName);
      writeToJson(value, jsonWriter);
    }
  }

  private static final class JsonListItemWriter implements ListItemWriter {
    private final JsonWriter jsonWriter;
    private final ScalarTypeAdapters scalarTypeAdapters;

    JsonListItemWriter(JsonWriter jsonWriter, ScalarTypeAdapters scalarTypeAdapters) {
      this.jsonWriter = jsonWriter;
      this.scalarTypeAdapters = scalarTypeAdapters;
    }

    @Override public void writeString(String value) throws IOException {
      if (value == null) {
        jsonWriter.nullValue();
      } else {
        jsonWriter.value(value);
      }
    }

    @Override public void writeInt(Integer value) throws IOException {
      if (value == null) {
        jsonWriter.nullValue();
      } else {
        jsonWriter.value(value);
      }
    }

    @Override public void writeLong(Long value) throws IOException {
      if (value == null) {
        jsonWriter.nullValue();
      } else {
        jsonWriter.value(value);
      }
    }

    @Override public void writeDouble(Double value) throws IOException {
      if (value == null) {
        jsonWriter.nullValue();
      } else {
        jsonWriter.value(value);
      }
    }

    @Override public void writeNumber(Number value) throws IOException {
      if (value == null) {
        jsonWriter.nullValue();
      } else {
        jsonWriter.value(value);
      }
    }

    @Override public void writeBoolean(Boolean value) throws IOException {
      if (value == null) {
        jsonWriter.nullValue();
      } else {
        jsonWriter.value(value);
      }
    }

    @Override public void writeMap(Map<String, Object> value) throws IOException {
      writeToJson(value, jsonWriter);
    }

    @SuppressWarnings("unchecked")
    @Override public void writeCustom(ScalarType scalarType, Object value) throws IOException {
      if (value == null) {
        jsonWriter.nullValue();
      } else {
        CustomTypeAdapter customTypeAdapter = scalarTypeAdapters.adapterFor(scalarType);
        CustomTypeValue customTypeValue = customTypeAdapter.encode(value);
        if (customTypeValue instanceof CustomTypeValue.GraphQLString) {
          writeString(((CustomTypeValue.GraphQLString) customTypeValue).value);
        } else if (customTypeValue instanceof CustomTypeValue.GraphQLBoolean) {
          writeBoolean(((CustomTypeValue.GraphQLBoolean) customTypeValue).value);
        } else if (customTypeValue instanceof CustomTypeValue.GraphQLNumber) {
          writeNumber(((CustomTypeValue.GraphQLNumber) customTypeValue).value);
        } else if (customTypeValue instanceof CustomTypeValue.GraphQLJsonString) {
          writeString(((CustomTypeValue.GraphQLJsonString) customTypeValue).value);
        } else if (customTypeValue instanceof CustomTypeValue.GraphQLJson) {
          writeMap(((CustomTypeValue.GraphQLJson) customTypeValue).value);
        } else {
          throw new IllegalArgumentException("Unsupported custom value type: " + customTypeValue);
        }
      }
    }

    @Override public void writeObject(InputFieldMarshaller marshaller) throws IOException {
      if (marshaller == null) {
        jsonWriter.nullValue();
      } else {
        jsonWriter.beginObject();
        marshaller.marshal(new InputFieldJsonWriter(jsonWriter, scalarTypeAdapters));
        jsonWriter.endObject();
      }
    }

    @Override public void writeList(ListWriter listWriter) throws IOException {
      if (listWriter == null) {
        jsonWriter.nullValue();
      } else {
        jsonWriter.beginArray();
        listWriter.write(new JsonListItemWriter(jsonWriter, scalarTypeAdapters));
        jsonWriter.endArray();
      }
    }
  }
}