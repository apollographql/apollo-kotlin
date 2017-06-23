package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.ResponseField;

import java.util.Map;

public final class MapFieldValueResolver implements FieldValueResolver<Map<String, Object>> {

  @SuppressWarnings("unchecked") @Override public <T> T valueFor(Map<String, Object> map, ResponseField field) {
    return (T) map.get(field.responseName());
  }
}
