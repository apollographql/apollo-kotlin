package com.apollographql.android.cache.normalized.sql;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

class FieldsAdapter {
  private final JsonAdapter<Map<String, Object>> adapter;

  private FieldsAdapter(Moshi moshi) {
    Type type = Types.newParameterizedType(Map.class, String.class, Object.class);

    adapter = moshi.adapter(type);
  }

  public static FieldsAdapter create(Moshi moshi) {
    return new FieldsAdapter(moshi);
  }

  String toJson(Map<String, Object> fields) {
    return adapter.toJson(fields);
  }

  Map<String, Object> fromJson(String fields) {
    try {
      return adapter.fromJson(fields);
    } catch (IOException e) {
      return new LinkedHashMap<String, Object>();
    }
  }
}
