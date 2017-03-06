package com.apollographql.android.impl;

import com.apollographql.android.api.graphql.Field;

import java.util.Map;

public class MapFieldValueResolver implements FieldValueResolver<Map<String, Object>> {

  @SuppressWarnings("unchecked") @Override public <T> T valueFor(Map<String, Object> map, Field field) {
    return (T) map.get(field.responseName());
  }
}
