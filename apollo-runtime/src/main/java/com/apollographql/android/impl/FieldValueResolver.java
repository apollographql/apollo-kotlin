package com.apollographql.android.impl;

import com.apollographql.android.api.graphql.Field;

interface FieldValueResolver<R> {
  <T> T valueFor(R recordSet, Field field);
}
