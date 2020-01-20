package com.apollographql.apollo.api.internal;

import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseFieldMarshaller;
import com.apollographql.apollo.api.ResponseWriter;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.json.JsonWriter;
import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.CustomTypeValue;
import com.apollographql.apollo.response.ScalarTypeAdapters;
import okio.Buffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.api.internal.json.Utils.writeToJson;

public class SimpleResponseWriter implements ResponseWriter {
  private final Map<String, Object> data;
  private final ScalarTypeAdapters scalarTypeAdapters;

  public SimpleResponseWriter(@NotNull ScalarTypeAdapters scalarTypeAdapters) {
    this.data = new LinkedHashMap<>();
    this.scalarTypeAdapters = checkNotNull(scalarTypeAdapters, "scalarTypeAdapters == null");
  }

  public String toJson(String indent) throws IOException {
    final Buffer buffer = new Buffer();

    final JsonWriter jsonWriter = JsonWriter.of(buffer);
    jsonWriter.setIndent(indent);
    jsonWriter.beginObject();
    jsonWriter.name("data");

    writeToJson(data, jsonWriter);

    jsonWriter.endObject();
    jsonWriter.close();

    return buffer.readUtf8();
  }

  @Override public void writeString(@NotNull ResponseField field, @Nullable String value) {
    data.put(field.responseName(), value);
  }

  @Override public void writeInt(@NotNull ResponseField field, @Nullable Integer value) {
    data.put(field.responseName(), value);
  }

  @Override public void writeLong(@NotNull ResponseField field, @Nullable Long value) {
    data.put(field.responseName(), value);
  }

  @Override public void writeDouble(@NotNull ResponseField field, @Nullable Double value) {
    data.put(field.responseName(), value);
  }

  @Override public void writeBoolean(@NotNull ResponseField field, @Nullable Boolean value) {
    data.put(field.responseName(), value);
  }

  @SuppressWarnings("unchecked")
  @Override public void writeCustom(@NotNull ResponseField.CustomTypeField field, @Nullable Object value) {
    if (value == null) {
      data.put(field.responseName(), null);
    } else {
      final CustomTypeAdapter typeAdapter = scalarTypeAdapters.adapterFor(field.scalarType());
      final CustomTypeValue customTypeValue = typeAdapter.encode(value);
      data.put(field.responseName(), customTypeValue.value);
    }
  }

  @Override public void writeObject(@NotNull ResponseField field, @Nullable ResponseFieldMarshaller marshaller) {
    if (marshaller == null) {
      data.put(field.responseName(), null);
    } else {
      SimpleResponseWriter objectResponseWriter = new SimpleResponseWriter(scalarTypeAdapters);
      marshaller.marshal(objectResponseWriter);
      data.put(field.responseName(), objectResponseWriter.data);
    }
  }

  @Override public void writeFragment(@Nullable ResponseFieldMarshaller marshaller) {
    if (marshaller != null) {
      marshaller.marshal(this);
    }
  }

  @Override public <T> void writeList(@NotNull ResponseField field, @Nullable List<T> values, @NotNull ListWriter<T> listWriter) {
    if (values == null) {
      data.put(field.responseName(), null);
    } else {
      CustomListItemWriter listItemWriter = new CustomListItemWriter(scalarTypeAdapters);
      listWriter.write(values, listItemWriter);
      data.put(field.responseName(), listItemWriter.data);
    }
  }

  private static class CustomListItemWriter implements ListItemWriter {
    private final List<Object> data;
    private final ScalarTypeAdapters scalarTypeAdapters;

    CustomListItemWriter(ScalarTypeAdapters scalarTypeAdapters) {
      this.data = new ArrayList<>();
      this.scalarTypeAdapters = scalarTypeAdapters;
    }

    @Override public void writeString(@Nullable String value) {
      data.add(value);
    }

    @Override public void writeInt(@Nullable Integer value) {
      data.add(value);
    }

    @Override public void writeLong(@Nullable Long value) {
      data.add(value);
    }

    @Override public void writeDouble(@Nullable Double value) {
      data.add(value);
    }

    @Override public void writeBoolean(@Nullable Boolean value) {
      data.add(value);
    }

    @SuppressWarnings("unchecked")
    @Override public void writeCustom(@NotNull ScalarType scalarType, @Nullable Object value) {
      if (value == null) {
        data.add(null);
      } else {
        final CustomTypeAdapter typeAdapter = scalarTypeAdapters.adapterFor(scalarType);
        final CustomTypeValue customTypeValue = typeAdapter.encode(value);
        data.add(customTypeValue.value);
      }
    }

    @Override public void writeObject(@Nullable ResponseFieldMarshaller marshaller) {
      if (marshaller == null) {
        data.add(null);
      } else {
        SimpleResponseWriter objectResponseWriter = new SimpleResponseWriter(scalarTypeAdapters);
        marshaller.marshal(objectResponseWriter);
        data.add(objectResponseWriter.data);
      }
    }

    @Override public <T> void writeList(@Nullable List<T> items, @NotNull ListWriter<T> listWriter) {
      if (items == null) {
        data.add(null);
      } else {
        CustomListItemWriter listItemWriter = new CustomListItemWriter(scalarTypeAdapters);
        listWriter.write(items, listItemWriter);
        data.add(listItemWriter.data);
      }
    }
  }
}
