package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.internal.json.CacheJsonStreamReader;
import com.apollographql.apollo.internal.json.JsonWriter;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

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

  public String toJson(@NotNull Map<String, Object> fields) {
    checkNotNull(fields, "fields == null");
    Buffer buffer = new Buffer();
    JsonWriter jsonWriter = JsonWriter.of(buffer);
    jsonWriter.setSerializeNulls(true);

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

  private Map<String, Object> fromBufferSource(BufferedSource bufferedFieldSource) throws IOException {
    final CacheJsonStreamReader cacheJsonStreamReader =
        cacheJsonStreamReader(bufferedSourceJsonReader(bufferedFieldSource));
    return cacheJsonStreamReader.toMap();
  }

  public Map<String, Object> from(String jsonFieldSource) throws IOException {
    final BufferedSource bufferSource
        = Okio.buffer(Okio.source(new ByteArrayInputStream(jsonFieldSource.getBytes(Charset.defaultCharset()))));
    return fromBufferSource(bufferSource);
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
    } else if (value instanceof Map) {
      //noinspection unchecked
      Map<String, Object> fields = (Map) value;
      jsonWriter.beginObject();
      for (Map.Entry<String, Object> fieldEntry : fields.entrySet()) {
        jsonWriter.name(fieldEntry.getKey());
        writeJsonValue(fieldEntry.getValue(), jsonWriter);
      }
      jsonWriter.endObject();
    } else {
      throw new RuntimeException("Unsupported record value type: " + value.getClass());
    }
  }
}
