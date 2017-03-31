package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.Field;

public interface FieldValueResolver<R> {
  <T> T valueFor(R recordSet, Field field);
}
