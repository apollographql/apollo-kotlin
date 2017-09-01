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
import java.util.Map;

public final class CacheFieldValueResolver implements FieldValueResolver<Record> {
  private final ReadableStore readableCache;
  private final Operation.Variables variables;
  private final CacheKeyResolver cacheKeyResolver;
  private final CacheHeaders cacheHeaders;
  private final Map<String, Object> variableValues;

  public CacheFieldValueResolver(ReadableStore readableCache, Operation.Variables variables,
      CacheKeyResolver cacheKeyResolver, CacheHeaders cacheHeaders) {
    this.readableCache = readableCache;
    this.variables = variables;
    this.cacheKeyResolver = cacheKeyResolver;
    this.cacheHeaders = cacheHeaders;
    this.variableValues = variables.valueMap();
  }

  @SuppressWarnings("unchecked") @Override public <T> T valueFor(Record record, ResponseField field) {
    if (shouldSkip(field, variableValues)) {
      return null;
    }

    switch (field.type()) {
      case OBJECT:
        return (T) valueForObject(record, field);

      case LIST: {
        return (T) valueForList((List) fieldValue(record, field));
      }

      default:
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

    if (cacheReference != null) {
      Record referencedRecord = readableCache.read(cacheReference.key(), cacheHeaders);
      if (referencedRecord == null) {
        // we are unable to find record in the cache by reference,
        // means it was removed intentionally by using imperative store API or
        // evicted from LRU cache, we must prevent of further resolving cache response as it's broken
        throw new IllegalStateException("Cache MISS: failed to find record in cache by reference");
      }
      return referencedRecord;
    }

    return null;
  }

  @SuppressWarnings("unchecked") private List valueForList(List values) {
    if (values == null) {
      return null;
    }

    List result = new ArrayList();
    for (Object value : values) {
      if (value instanceof CacheReference) {
        CacheReference reference = (CacheReference) value;
        Record referencedRecord = readableCache.read(reference.key(), cacheHeaders);
        if (referencedRecord == null) {
          // we are unable to find record in the cache by reference,
          // means it was removed intentionally by using imperative store API or
          // evicted from LRU cache, we must prevent of further resolving cache response as it's broken
          throw new IllegalStateException("Cache MISS: failed to find record in cache by reference");
        }
        result.add(referencedRecord);
      } else if (value instanceof List) {
        result.add(valueForList((List) value));
      } else {
        result.add(value);
      }
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

  private static boolean shouldSkip(ResponseField field, Map<String, Object> variableValues) {
    for (ResponseField.Condition condition : field.conditions()) {
      if (condition instanceof ResponseField.BooleanCondition) {
        ResponseField.BooleanCondition booleanCondition = (ResponseField.BooleanCondition) condition;
        Boolean conditionValue = (Boolean) variableValues.get(booleanCondition.variableName());
        if (booleanCondition.inverted()) {
          // means it's a skip directive
          if (conditionValue == Boolean.TRUE) {
            return true;
          }
        } else {
          // means it's an include directive
          if (conditionValue == Boolean.FALSE) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
