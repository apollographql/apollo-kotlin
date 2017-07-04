package com.apollographql.apollo.internal.field;

import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheKeyResolver;
import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.internal.cache.normalized.ReadableStore;

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

  @SuppressWarnings("unchecked") @Override public <T> T valueFor(Record record, ResponseField field) {
    if (field.type() == ResponseField.Type.OBJECT) {
      return (T) valueForObject(record, field);
    } else if (field.type() == ResponseField.Type.OBJECT_LIST) {
      return (T) valueForObjectList(record, field);
    } else {
      return fieldValue(record, field);
    }
  }

  private Record valueForObject(Record record, ResponseField field) {
    CacheReference cacheReference;
    CacheKey fieldCacheKey = cacheKeyResolver.fromFieldArguments(field, variables);
    if (fieldCacheKey != CacheKey.NO_KEY) {
      cacheReference = new CacheReference(fieldCacheKey.key());
    } else {
      cacheReference = fieldValue(record, field);
    }
    return cacheReference != null ? readableCache.read(cacheReference.key(), cacheHeaders) : null;
  }

  private List<Record> valueForObjectList(Record record, ResponseField field) {
    List<CacheReference> values = fieldValue(record, field);
    if (values == null) {
      return null;
    }

    List<Record> result = new ArrayList<>();
    for (CacheReference reference : values) {
      result.add(readableCache.read(reference.key(), cacheHeaders));
    }
    return result;
  }

  @SuppressWarnings("unchecked") private <T> T fieldValue(Record record, ResponseField field) {
    String fieldKey = field.cacheKey(variables);
    if (!record.hasField(fieldKey)) {
      throw new NullPointerException("Missing value: " + field.fieldName());
    }
    return (T) record.field(fieldKey);
  }
}
