package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.ResponseField;

import java.io.IOException;

public interface FieldValueResolver<R> {
  <T> T valueFor(R recordSet, ResponseField field) throws IOException;
}
