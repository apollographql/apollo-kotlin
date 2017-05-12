package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.Field;

import java.io.IOException;

public interface FieldValueResolver<R> {
  <T> T valueFor(R recordSet, Field field) throws IOException;
}
