package com.apollographql.apollo.internal.response;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.internal.FieldValueResolver;
import com.apollographql.apollo.api.internal.ResolveDelegate;
import com.apollographql.apollo.api.internal.ResponseReader;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public final class RealResponseReader<R> implements ResponseReader {
  final Operation.Variables operationVariables;
  private final R recordSet;
  final ScalarTypeAdapters scalarTypeAdapters;
  final FieldValueResolver<R> fieldValueResolver;
  final ResolveDelegate<R> resolveDelegate;
  private final Map<String, Object> variableValues;

  public RealResponseReader(Operation.Variables operationVariables, R recordSet,
      FieldValueResolver<R> fieldValueResolver, ScalarTypeAdapters scalarTypeAdapters,
      ResolveDelegate<R> resolveDelegate) {
    this.operationVariables = operationVariables;
    this.recordSet = recordSet;
    this.fieldValueResolver = fieldValueResolver;
    this.scalarTypeAdapters = scalarTypeAdapters;
    this.resolveDelegate = resolveDelegate;
    this.variableValues = operationVariables.valueMap();
  }

  @Override public String readString(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    if (value == null) {
      resolveDelegate.didResolveNull();
    } else {
      resolveDelegate.didResolveScalar(value);
    }
    didResolve(field);
    return value;
  }

  @Override public Integer readInt(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    if (value == null) {
      resolveDelegate.didResolveNull();
    } else {
      resolveDelegate.didResolveScalar(value);
    }
    didResolve(field);
    return value != null ? value.intValue() : null;
  }

  @Override public Long readLong(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    if (value == null) {
      resolveDelegate.didResolveNull();
    } else {
      resolveDelegate.didResolveScalar(value);
    }
    didResolve(field);
    return value != null ? value.longValue() : null;
  }

  @Override public Double readDouble(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    if (value == null) {
      resolveDelegate.didResolveNull();
    } else {
      resolveDelegate.didResolveScalar(value);
    }
    didResolve(field);
    return value != null ? value.doubleValue() : null;
  }

  @Override public Boolean readBoolean(ResponseField field) {
    if (shouldSkip(field)) {
      return null;
    }

    Boolean value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    if (value == null) {
      resolveDelegate.didResolveNull();
    } else {
      resolveDelegate.didResolveScalar(value);
    }
    didResolve(field);
    return value;
  }

  @SuppressWarnings("unchecked") @Override
  public <T> T readObject(ResponseField field, ResponseReader.ObjectReader<T> objectReader) {
    if (shouldSkip(field)) {
      return null;
    }

    R value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    resolveDelegate.willResolveObject(field, value);
    final T parsedValue;
    if (value == null) {
      resolveDelegate.didResolveNull();
      parsedValue = null;
    } else {
      parsedValue = (T) objectReader.read(new RealResponseReader(operationVariables, value, fieldValueResolver,
          scalarTypeAdapters, resolveDelegate));
    }
    resolveDelegate.didResolveObject(field, value);
    didResolve(field);
    return parsedValue;
  }

  @SuppressWarnings("unchecked")
  @Override public <T> List<T> readList(ResponseField field, ListReader<T> listReader) {
    if (shouldSkip(field)) {
      return null;
    }

    List values = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, values);

    willResolve(field, values);
    final List<T> result;
    if (values == null) {
      resolveDelegate.didResolveNull();
      result = null;
    } else {
      result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        resolveDelegate.willResolveElement(i);
        Object value = values.get(i);
        if (value == null) {
          result.add(null);
          resolveDelegate.didResolveNull();
        } else {
          T item = (T) listReader.read(new ListItemReader(field, value));
          result.add(item);
        }
        resolveDelegate.didResolveElement(i);
      }
      resolveDelegate.didResolveList(values);
    }
    didResolve(field);
    return result != null ? Collections.unmodifiableList(result) : null;
  }

  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
  @Override public <T> T readCustomType(ResponseField.CustomTypeField field) {
    if (shouldSkip(field)) {
      return null;
    }

    Object value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    final T result;
    if (value == null) {
      resolveDelegate.didResolveNull();
      result = null;
    } else {
      CustomTypeAdapter<T> typeAdapter = scalarTypeAdapters.adapterFor(field.scalarType());
      result = typeAdapter.decode(CustomTypeValue.fromRawValue(value));
      checkValue(field, result);
      resolveDelegate.didResolveScalar(value);
    }
    didResolve(field);
    return result;
  }

  @Override public <T> T readFragment(ResponseField field, ObjectReader<T> objectReader) {
    if (shouldSkip(field)) {
      return null;
    }

    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    if (value == null) {
      resolveDelegate.didResolveNull();
      didResolve(field);
      return null;
    } else {
      resolveDelegate.didResolveScalar(value);
      didResolve(field);
      if (field.type() == ResponseField.Type.FRAGMENT) {
        for (ResponseField.Condition condition : field.conditions()) {
          if (condition instanceof ResponseField.TypeNameCondition) {
            if (((ResponseField.TypeNameCondition) condition).typeNames().contains(value)) {
              return objectReader.read(this);
            }
          }
        }
      }
      return null;
    }
  }

  private boolean shouldSkip(ResponseField field) {
    for (ResponseField.Condition condition : field.conditions()) {
      if (condition instanceof ResponseField.BooleanCondition) {
        ResponseField.BooleanCondition booleanCondition = (ResponseField.BooleanCondition) condition;
        Boolean conditionValue = (Boolean) variableValues.get(booleanCondition.variableName());
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

  private void willResolve(ResponseField field, Object value) {
    resolveDelegate.willResolve(field, operationVariables, value);
  }

  private void didResolve(ResponseField field) {
    resolveDelegate.didResolve(field, operationVariables);
  }

  private void checkValue(ResponseField field, Object value) {
    if (!field.getOptional() && value == null) {
      throw new NullPointerException("corrupted response reader, expected non null value for " + field.getFieldName());
    }
  }

  private class ListItemReader implements ResponseReader.ListItemReader {
    private final ResponseField field;
    private final Object value;

    ListItemReader(ResponseField field, Object value) {
      this.field = field;
      this.value = value;
    }

    @Override public String readString() {
      resolveDelegate.didResolveScalar(value);
      return (String) value;
    }

    @Override public int readInt() {
      resolveDelegate.didResolveScalar(value);
      return ((BigDecimal) value).intValue();
    }

    @Override public long readLong() {
      resolveDelegate.didResolveScalar(value);
      return ((BigDecimal) value).longValue();
    }

    @Override public double readDouble() {
      resolveDelegate.didResolveScalar(value);
      return ((BigDecimal) value).doubleValue();
    }

    @Override public boolean readBoolean() {
      resolveDelegate.didResolveScalar(value);
      return (Boolean) value;
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    @Override public <T> T readCustomType(ScalarType scalarType) {
      CustomTypeAdapter<T> typeAdapter = scalarTypeAdapters.adapterFor(scalarType);
      resolveDelegate.didResolveScalar(value);
      return typeAdapter.decode(CustomTypeValue.fromRawValue(value));
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T readObject(ObjectReader<T> objectReader) {
      R value = (R) this.value;
      resolveDelegate.willResolveObject(field, value);
      T item = (T) objectReader.read(new RealResponseReader<R>(operationVariables, value, fieldValueResolver,
          scalarTypeAdapters, resolveDelegate));
      resolveDelegate.didResolveObject(field, value);
      return item;
    }

    @Override public <T> List<T> readList(ListReader<T> listReader) {
      List values = (List) value;
      if (values == null) {
        return null;
      }

      List<T> result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        resolveDelegate.willResolveElement(i);
        Object value = values.get(i);
        if (value == null) {
          result.add(null);
          resolveDelegate.didResolveNull();
        } else {
          T item = (T) listReader.read(new ListItemReader(field, value));
          result.add(item);
        }
        resolveDelegate.didResolveElement(i);
      }
      resolveDelegate.didResolveList(values);
      return Collections.unmodifiableList(result);
    }
  }
}
