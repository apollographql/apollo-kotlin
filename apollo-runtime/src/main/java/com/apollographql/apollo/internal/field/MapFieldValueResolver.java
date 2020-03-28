package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.internal.FieldValueResolver;

import java.util.Map;

public final class MapFieldValueResolver implements FieldValueResolver<Map<String, Object>> {

  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
  @Override public <T> T valueFor(Map<String, Object> map, ResponseField field) {
    return (T) map.get(field.getResponseName());
  }
}
