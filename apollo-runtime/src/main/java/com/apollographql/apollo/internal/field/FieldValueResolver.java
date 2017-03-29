package com.apollographql.apollo.internal.field;

import com.apollographql.android.api.graphql.Field;

public interface FieldValueResolver<R> {
  <T> T valueFor(R recordSet, Field field);
}
