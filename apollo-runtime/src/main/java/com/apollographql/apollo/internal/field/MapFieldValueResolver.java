package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.Field;

import java.io.IOException;
import java.util.Map;

public final class MapFieldValueResolver implements FieldValueResolver<Map<String, Object>> {

  @SuppressWarnings("unchecked") @Override public <T> T valueFor(Map<String, Object> map, Field field)
      throws IOException {
    if (!map.containsKey(field.responseName())) {
      throw new IOException("Missing value: " + field.responseName());
    }
    return (T) map.get(field.responseName());
  }
}
