package com.apollographql.apollo.internal.json;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.InputFieldMarshaller;
import com.apollographql.apollo.api.InputFieldWriter;
import com.apollographql.apollo.api.ScalarType;

import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;

public class InputFieldJsonWriter implements InputFieldWriter {
  private final JsonWriter jsonWriter;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

  public InputFieldJsonWriter(JsonWriter jsonWriter, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.jsonWriter = jsonWriter;
    this.customTypeAdapters = customTypeAdapters;
  }

  @Override public void writeString(@Nonnull String fieldName, String value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeInt(@Nonnull String fieldName, Integer value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeLong(@Nonnull String fieldName, Long value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeDouble(@Nonnull String fieldName, Double value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeBoolean(@Nonnull String fieldName, Boolean value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (value != null) {
      jsonWriter.name(fieldName).value(value);
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeCustom(@Nonnull String fieldName, ScalarType scalarType, Object value) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    CustomTypeAdapter customTypeAdapter = customTypeAdapters.get(scalarType);
    if (customTypeAdapter != null) {
      writeString(fieldName, customTypeAdapter.encode(value));
    } else {
      writeString(fieldName, value != null ? value.toString() : null);
    }
  }

  @Override public void writeObject(@Nonnull String fieldName, InputFieldMarshaller marshaller) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (marshaller != null) {
      jsonWriter.name(fieldName).beginObject();
      marshaller.marshal(this);
      jsonWriter.endObject();
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  @Override public void writeList(@Nonnull String fieldName, ListWriter listWriter) throws IOException {
    checkNotNull(fieldName, "fieldName == null");
    if (listWriter != null) {
      jsonWriter.name(fieldName).beginArray();
      listWriter.write(new JsonListItemWriter(jsonWriter, customTypeAdapters));
      jsonWriter.endArray();
    } else {
      jsonWriter.name(fieldName).nullValue();
    }
  }

  private static final class JsonListItemWriter implements ListItemWriter {
    private final JsonWriter jsonWriter;
    private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

    JsonListItemWriter(JsonWriter jsonWriter, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
      this.jsonWriter = jsonWriter;
      this.customTypeAdapters = customTypeAdapters;
    }

    @Override public void writeString(String value) throws IOException {
      if (value != null) {
        jsonWriter.value(value);
      }
    }

    @Override public void writeInt(Integer value) throws IOException {
      if (value != null) {
        jsonWriter.value(value);
      }
    }

    @Override public void writeLong(Long value) throws IOException {
      if (value != null) {
        jsonWriter.value(value);
      }
    }

    @Override public void writeDouble(Double value) throws IOException {
      if (value != null) {
        jsonWriter.value(value);
      }
    }

    @Override public void writeBoolean(Boolean value) throws IOException {
      if (value != null) {
        jsonWriter.value(value);
      }
    }

    @Override public void writeCustom(ScalarType scalarType, Object value) throws IOException {
      if (value == null) {
        return;
      }

      CustomTypeAdapter customTypeAdapter = customTypeAdapters.get(scalarType);
      if (customTypeAdapter != null) {
        writeString(customTypeAdapter.encode(value));
      } else {
        writeString(value.toString());
      }
    }

    @Override public void writeObject(InputFieldMarshaller marshaller) throws IOException {
      if (marshaller != null) {
        jsonWriter.beginObject();
        marshaller.marshal(new InputFieldJsonWriter(jsonWriter, customTypeAdapters));
        jsonWriter.endObject();
      }
    }
  }
}