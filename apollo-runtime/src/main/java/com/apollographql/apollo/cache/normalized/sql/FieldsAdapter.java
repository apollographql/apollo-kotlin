package com.apollographql.apollo.cache.normalized.sql;

import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.internal.json.CacheJsonStreamReader;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Map;

import okio.BufferedSource;
import okio.Okio;

import static com.apollographql.apollo.internal.json.ApolloJsonReader.bufferedSourceJsonReader;
import static com.apollographql.apollo.internal.json.ApolloJsonReader.cacheJsonStreamReader;

public final class FieldsAdapter {
  private final JsonAdapter<Map<String, Object>> serializationAdapter;

  private FieldsAdapter(Moshi moshi) {
    Type type = Types.newParameterizedType(Map.class, String.class, Object.class);
    serializationAdapter = moshi.adapter(type);
  }

  public static FieldsAdapter create() {
    Moshi moshi = new Moshi.Builder()
        .add(CacheReference.class, new CacheReferenceAdapter())
        .add(BigDecimal.class, new BigDecimalAdapter())
        .build();
    return new FieldsAdapter(moshi);
  }

  public String toJson(Map<String, Object> fields) {
    return serializationAdapter.toJson(fields);
  }

  public Map<String, Object> from(BufferedSource bufferedFieldSource) throws IOException {
    final CacheJsonStreamReader cacheJsonStreamReader =
        cacheJsonStreamReader(bufferedSourceJsonReader(bufferedFieldSource));
    return cacheJsonStreamReader.buffer();
  }

  public Map<String, Object> from(String jsonFieldSource) throws IOException {
    final BufferedSource bufferSource = Okio.buffer(Okio.source(new ByteArrayInputStream(jsonFieldSource.getBytes())));
    return from(bufferSource);
  }

  private static class CacheReferenceAdapter extends JsonAdapter<CacheReference> {

    @Override public CacheReference fromJson(JsonReader reader) throws IOException {
      throw new IllegalStateException(this.getClass().getName() + " should only be used for serialization.");
    }

    @Override public void toJson(JsonWriter writer, CacheReference value) throws IOException {
      writer.value(value.serialize());
    }
  }

  private static class BigDecimalAdapter extends JsonAdapter<BigDecimal> {

    @Override public BigDecimal fromJson(JsonReader reader) throws IOException {
      throw new IllegalStateException(this.getClass().getName() + " should only be used for serialization.");
    }

    @Override public void toJson(JsonWriter writer, BigDecimal value) throws IOException {
      writer.value(value);
    }
  }
}
