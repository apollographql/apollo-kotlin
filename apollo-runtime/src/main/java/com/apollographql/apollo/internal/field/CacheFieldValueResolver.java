package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.cache.normalized.ReadableStore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class CacheFieldValueResolver implements FieldValueResolver<Record> {
  private final ReadableStore readableCache;
  private final Operation.Variables variables;
  private final CacheKeyResolver cacheKeyResolver;
  private final CacheHeaders cacheHeaders;

  public CacheFieldValueResolver(ReadableStore readableCache, Operation.Variables variables,
      CacheKeyResolver cacheKeyResolver, CacheHeaders cacheHeaders) {
    this.readableCache = readableCache;
    this.variables = variables;
    this.cacheKeyResolver = cacheKeyResolver;
    this.cacheHeaders = cacheHeaders;
  }

  @SuppressWarnings("unchecked") @Override public <T> T valueFor(Record record, Field field)
      throws IOException {
    if (field instanceof Field.ObjectField) {
      return (T) valueFor(record, (Field.ObjectField) field);
    } else if (field instanceof Field.ScalarListField) {
      return fieldValue(record, field);
    } else if (field instanceof Field.ObjectListField) {
      return (T) valueFor(record, (Field.ObjectListField) field);
    } else {
      return fieldValue(record, field);
    }
  }

  private Record valueFor(Record record, Field.ObjectField field) throws IOException {
    CacheReference cacheReference;
    CacheKey fieldCacheKey = cacheKeyResolver.fromFieldArguments(field, variables);
    if (fieldCacheKey != CacheKey.NO_KEY) {
      cacheReference = new CacheReference(fieldCacheKey.key());
    } else {
      cacheReference = fieldValue(record, field);
    }
    return cacheReference != null ? readableCache.read(cacheReference.key(), cacheHeaders) : null;
  }

  private List<Record> valueFor(Record record, Field.ObjectListField field) throws IOException {
    List<CacheReference> values = fieldValue(record, field);
    List<Record> result = new ArrayList<>();
    for (CacheReference reference : values) {
      result.add(readableCache.read(reference.key(), cacheHeaders));
    }
    return result;
  }

  @SuppressWarnings("unchecked") private <T> T fieldValue(Record record, Field field) throws IOException {
    String fieldKey = field.cacheKey(variables);
    if (!record.hasField(fieldKey)) {
      throw new IOException("Missing value: " + field.fieldName());
    }
    return (T) record.field(fieldKey);
  }
}
