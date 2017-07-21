package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.internal.json.CacheJsonStreamReader;
import com.apollographql.apollo.internal.json.JsonWriter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.internal.json.ApolloJsonReader.bufferedSourceJsonReader;
import static com.apollographql.apollo.internal.json.ApolloJsonReader.cacheJsonStreamReader;

/**
 * An adapter used to serialize and deserialize Record fields. Record object types will be serialized to
 * {@link CacheReference}.
 */
public final class RecordFieldJsonAdapter {

  public static RecordFieldJsonAdapter create() {
    return new RecordFieldJsonAdapter();
  }

  private RecordFieldJsonAdapter() {
  }

  public String toJson(@Nonnull Map<String, Object> fields) {
    checkNotNull(fields, "fields == null");
    Buffer buffer = new Buffer();
    JsonWriter jsonWriter = JsonWriter.of(buffer);

    try {
      jsonWriter.beginObject();
      for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
        String key = fieldEntry.getKey();
        Object value = fieldEntry.getValue();
        jsonWriter.name(key);
        writeJsonValue(value, jsonWriter);
      }
      jsonWriter.endObject();
      jsonWriter.close();
      return buffer.readUtf8();
    } catch (IOException e) {
      // should never happen as we are working with mem buffer
      throw new RuntimeException(e);
    }
  }

  public Map<String, Object> from(BufferedSource bufferedFieldSource) throws IOException {
    final CacheJsonStreamReader cacheJsonStreamReader =
        cacheJsonStreamReader(bufferedSourceJsonReader(bufferedFieldSource));
    return cacheJsonStreamReader.toMap();
  }

  public Map<String, Object> from(String jsonFieldSource) throws IOException {
    final BufferedSource bufferSource = Okio.buffer(Okio.source(new ByteArrayInputStream(jsonFieldSource.getBytes())));
    return from(bufferSource);
  }

  private static void writeJsonValue(Object value, JsonWriter jsonWriter) throws IOException {
    if (value == null) {
      jsonWriter.nullValue();
    } else if (value instanceof String) {
      jsonWriter.value((String) value);
    } else if (value instanceof Boolean) {
      jsonWriter.value((boolean) value);
    } else if (value instanceof Number) {
      jsonWriter.value((Number) value);
    } else if (value instanceof CacheReference) {
      jsonWriter.value(((CacheReference) value).serialize());
    } else if (value instanceof List) {
      jsonWriter.beginArray();
      for (Object item : (List) value) {
        writeJsonValue(item, jsonWriter);
      }
      jsonWriter.endArray();
    } else {
      throw new RuntimeException("Unsupported record value type: " + value.getClass());
    }
  }
}
