package com.apollographql.apollo.api.internal;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SimpleResponseReader implements ResponseReader {
  private final Map<String, Object> recordSet;
  private final ScalarTypeAdapters scalarTypeAdapters;
  private final Map<String, Object> variableValues;

  public SimpleResponseReader(Map<String, Object> recordSet, Operation.Variables variables, ScalarTypeAdapters scalarTypeAdapters) {
    this(recordSet, variables.valueMap(), scalarTypeAdapters);
  }

  private SimpleResponseReader(Map<String, Object> recordSet, Map<String, Object> variableValues, ScalarTypeAdapters scalarTypeAdapters) {
    this.recordSet = recordSet;
    this.variableValues = variableValues;
    this.scalarTypeAdapters = scalarTypeAdapters;
  }

  @Override public String readString(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    final String value = valueFor(recordSet, field);
    return checkValue(field, value);
  }

  @Override public Integer readInt(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    final BigDecimal value = valueFor(recordSet, field);
    checkValue(field, value);
    return value != null ? value.intValue() : null;
  }

  @Override public Long readLong(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    final BigDecimal value = valueFor(recordSet, field);
    checkValue(field, value);
    return value != null ? value.longValue() : null;
  }

  @Override public Double readDouble(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    final BigDecimal value = valueFor(recordSet, field);
    checkValue(field, value);
    return value != null ? value.doubleValue() : null;
  }

  @Override public Boolean readBoolean(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    final Boolean value = valueFor(recordSet, field);
    return checkValue(field, value);
  }

  @Override public <T> T readObject(ResponseField field, ResponseReader.ObjectReader<T> objectReader) {
    if (shouldSkip(field)) {
      return null;
    }

    final Map<String, Object> value = valueFor(recordSet, field);
    checkValue(field, value);
    return value != null ? objectReader.read(new SimpleResponseReader(value, variableValues, scalarTypeAdapters)) : null;
  }

  @Override public <T> List<T> readList(ResponseField field, ListReader<T> listReader) {
    if (shouldSkip(field)) {
      return null;
    }

    final List values = valueFor(recordSet, field);
    checkValue(field, values);
    final List<T> result;
    if (values == null) {
      result = null;
    } else {
      result = new ArrayList<>();
      for (Object value : values) {
        if (value == null) {
          result.add(null);
        } else {
          final T item = listReader.read(new ListItemReader(field, value));
          result.add(item);
        }
      }
    }
    return result != null ? Collections.unmodifiableList(result) : null;
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  @Override public <T> T readCustomType(ResponseField.CustomTypeField field) {
    if (shouldSkip(field)) {
      return null;
    }

    final Object value = valueFor(recordSet, field);
    checkValue(field, value);
    final T result;
    if (value == null) {
      result = null;
    } else {
      final CustomTypeAdapter<T> typeAdapter = scalarTypeAdapters.adapterFor(field.scalarType());
      result = typeAdapter.decode(CustomTypeValue.fromRawValue(value));
    }
    return checkValue(field, result);
  }

  @Override public <T> T readConditional(ResponseField field, ConditionalTypeReader<T> conditionalTypeReader) {
    if (shouldSkip(field)) {
      return null;
    }

    final String value = valueFor(recordSet, field);
    checkValue(field, value);
    if (value == null) {
      return null;
    } else {
      if (field.type() == ResponseField.Type.INLINE_FRAGMENT) {
        for (ResponseField.Condition condition : field.conditions()) {
          if (condition instanceof ResponseField.TypeNameCondition) {
            if (((ResponseField.TypeNameCondition) condition).typeName().equals(value)) {
              return conditionalTypeReader.read(value, this);
            }
          }
        }
        return null;
      } else {
        return conditionalTypeReader.read(value, this);
      }
    }
  }

  private boolean shouldSkip(ResponseField field) {
    for (ResponseField.Condition condition : field.conditions()) {
      if (condition instanceof ResponseField.BooleanCondition) {
        final ResponseField.BooleanCondition booleanCondition = (ResponseField.BooleanCondition) condition;
        final Boolean conditionValue = (Boolean) variableValues.get(booleanCondition.variableName());
        if (booleanCondition.inverted()) {
          // means it's a skip directive
          if (Boolean.TRUE.equals(conditionValue)) {
            return true;
          }
        } else {
          // means it's an include directive
          if (Boolean.FALSE.equals(conditionValue)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private <V> V checkValue(ResponseField field, V value) {
    if (!field.optional() && value == null) {
      throw new NullPointerException("corrupted response reader, expected non null value for " + field.fieldName());
    }

    return value;
  }

  private class ListItemReader implements ResponseReader.ListItemReader {
    private final ResponseField field;
    private final Object value;

    ListItemReader(ResponseField field, Object value) {
      this.field = field;
      this.value = value;
    }

    @Override public String readString() {
      return (String) value;
    }

    @Override public Integer readInt() {
      return ((BigDecimal) value).intValue();
    }

    @Override public Long readLong() {
      return ((BigDecimal) value).longValue();
    }

    @Override public Double readDouble() {
      return ((BigDecimal) value).doubleValue();
    }

    @Override public Boolean readBoolean() {
      return (Boolean) value;
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    @Override public <T> T readCustomType(ScalarType scalarType) {
      final CustomTypeAdapter<T> typeAdapter = scalarTypeAdapters.adapterFor(scalarType);
      return typeAdapter.decode(CustomTypeValue.fromRawValue(value));
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T readObject(ObjectReader<T> objectReader) {
      final Map<String, Object> value = (Map<String, Object>) this.value;
      return objectReader.read(new SimpleResponseReader(value, variableValues, scalarTypeAdapters));
    }

    @Override public <T> List<T> readList(ListReader<T> listReader) {
      final List values = (List) value;
      if (values == null) {
        return null;
      }

      final List<T> result = new ArrayList<>();
      for (Object value : values) {
        if (value == null) {
          result.add(null);
        } else {
          final T item = (T) listReader.read(new ListItemReader(field, value));
          result.add(item);
        }
      }
      return Collections.unmodifiableList(result);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> T valueFor(Map<String, Object> map, ResponseField field) {
    return (T) map.get(field.responseName());
  }
}
