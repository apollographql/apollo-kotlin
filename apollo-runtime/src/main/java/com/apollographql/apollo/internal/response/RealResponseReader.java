package com.apollographql.apollo.internal.response;

import com.apollographql.apollo.api.CustomTypeAdapter;
import com.apollographql.apollo.api.CustomTypeValue;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.ScalarTypeAdapters;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.api.internal.ResponseReader;
import com.apollographql.apollo.internal.field.FieldValueResolver;
import org.jetbrains.annotations.NotNull;

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

  @Override public String readString(@NotNull ResponseField field) {
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

  @Override public Integer readInt(@NotNull ResponseField field) {
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

  @Override public Long readLong(@NotNull ResponseField field) {
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

  @Override public Double readDouble(@NotNull ResponseField field) {
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

  @Override public Boolean readBoolean(@NotNull ResponseField field) {
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
  public <T> T readObject(@NotNull ResponseField field, @NotNull ResponseReader.ObjectReader<T> objectReader) {
    if (shouldSkip(field)) {
      return null;
    }

    R value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);

    willResolve(field, value);
    resolveDelegate.willResolveObject(field, Optional.fromNullable(value));
    final T parsedValue;
    if (value == null) {
      resolveDelegate.didResolveNull();
      parsedValue = null;
    } else {
      parsedValue = objectReader.read(new RealResponseReader(operationVariables, value, fieldValueResolver, scalarTypeAdapters,
          resolveDelegate));
    }
    resolveDelegate.didResolveObject(field, Optional.fromNullable(value));
    didResolve(field);
    return parsedValue;
  }

  @Override public <T> List<T> readList(@NotNull ResponseField field, @NotNull ListReader<T> listReader) {
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
          T item = listReader.read(new ListItemReader(field, value));
          result.add(item);
        }
        resolveDelegate.didResolveElement(i);
      }
      resolveDelegate.didResolveList(values);
    }
    didResolve(field);
    return result != null ? Collections.unmodifiableList(result) : null;
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  @Override public <T> T readCustomType(@NotNull ResponseField.CustomTypeField field) {
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
      CustomTypeAdapter<T> typeAdapter = scalarTypeAdapters.adapterFor(field.getScalarType());
      result = typeAdapter.decode(CustomTypeValue.fromRawValue(value));
      checkValue(field, result);
      resolveDelegate.didResolveScalar(value);
    }
    didResolve(field);
    return result;
  }

  @Override public <T> T readFragment(@NotNull ResponseField field, @NotNull ObjectReader<T> objectReader) {
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

      if (field.getType() == ResponseField.Type.FRAGMENT) {
        for (ResponseField.Condition condition : field.getConditions()) {
          if (condition instanceof ResponseField.TypeNameCondition) {
            if (!((ResponseField.TypeNameCondition) condition).getTypeNames().contains(value)) {
              return null;
            }
          }
        }
        return objectReader.read(this);
      } else {
        return null;
      }
    }
  }

  private boolean shouldSkip(ResponseField field) {
    for (ResponseField.Condition condition : field.getConditions()) {
      if (condition instanceof ResponseField.BooleanCondition) {
        ResponseField.BooleanCondition booleanCondition = (ResponseField.BooleanCondition) condition;
        Boolean conditionValue = (Boolean) variableValues.get(booleanCondition.getVariableName());
        if (booleanCondition.getInverted()) {
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
    resolveDelegate.willResolve(field, operationVariables, Optional.fromNullable(value));
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

    @NotNull @Override public String readString() {
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

    @SuppressWarnings("TypeParameterUnusedInFormals")
    @NotNull @Override public <T> T readCustomType(@NotNull ScalarType scalarType) {
      CustomTypeAdapter<T> typeAdapter = scalarTypeAdapters.adapterFor(scalarType);
      resolveDelegate.didResolveScalar(value);
      return typeAdapter.decode(CustomTypeValue.fromRawValue(value));
    }

    @SuppressWarnings("unchecked")
    @NotNull @Override public <T> T readObject(ObjectReader<T> objectReader) {
      R value = (R) this.value;
      resolveDelegate.willResolveObject(field, Optional.fromNullable(value));
      T item = objectReader.read(new RealResponseReader<R>(operationVariables, value, fieldValueResolver, scalarTypeAdapters,
          resolveDelegate));
      resolveDelegate.didResolveObject(field, Optional.fromNullable(value));
      return item;
    }

    @NotNull @Override public <T> List<T> readList(@NotNull ListReader<T> listReader) {
      List values = (List) value;
      List<T> result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        resolveDelegate.willResolveElement(i);
        Object value = values.get(i);
        if (value == null) {
          result.add(null);
          resolveDelegate.didResolveNull();
        } else {
          T item = listReader.read(new ListItemReader(field, value));
          result.add(item);
        }
        resolveDelegate.didResolveElement(i);
      }
      resolveDelegate.didResolveList(values);
      return Collections.unmodifiableList(result);
    }
  }
}
