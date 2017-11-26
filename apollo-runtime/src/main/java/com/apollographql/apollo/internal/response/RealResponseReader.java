package com.apollographql.apollo.internal.response;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.internal.field.FieldValueResolver;
import com.apollographql.apollo.response.ScalarTypeAdapters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") public final class RealResponseReader<R> implements ResponseReader {
  private final Operation.Variables operationVariables;
  private final R recordSet;
  private final com.apollographql.apollo.response.ScalarTypeAdapters scalarTypeAdapters;
  private final FieldValueResolver<R> fieldValueResolver;
  private final ResolveDelegate<R> resolveDelegate;
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

    willResolve(field);
    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);
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

    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);
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

    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);
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

    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);
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

    willResolve(field);
    Boolean value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);
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

    willResolve(field);
    R value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);
    resolveDelegate.willResolveObject(field, Optional.fromNullable(value));
    final T parsedValue;
    if (value == null) {
      resolveDelegate.didResolveNull();
      parsedValue = null;
    } else {
      parsedValue = (T) objectReader.read(new RealResponseReader(operationVariables, value, fieldValueResolver,
          scalarTypeAdapters, resolveDelegate));
    }
    resolveDelegate.didResolveObject(field, Optional.fromNullable(value));
    didResolve(field);
    return parsedValue;
  }

  @SuppressWarnings("unchecked")
  @Override public <T> List<T> readList(ResponseField field, ListReader<T> listReader) {
    if (shouldSkip(field)) {
      return null;
    }

    willResolve(field);
    List values = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, values);
    final List<T> result;
    if (values == null) {
      resolveDelegate.didResolveNull();
      result = null;
    } else {
      result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        resolveDelegate.willResolveElement(i);
        Object value = values.get(i);
        if (value != null) {
          T item = (T) listReader.read(new ListItemReader(field, value));
          if (item != null) {
            result.add(item);
          }
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

    willResolve(field);
    Object value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);
    final T result;
    if (value == null) {
      resolveDelegate.didResolveNull();
      result = null;
    } else {
      CustomTypeAdapter<T> typeAdapter = scalarTypeAdapters.adapterFor(field.scalarType());
      result = typeAdapter.decode(value.toString());
      checkValue(field, result);
      resolveDelegate.didResolveScalar(value);
    }
    didResolve(field);
    return result;
  }

  @Override
  public <T> T readConditional(ResponseField field, ConditionalTypeReader<T> conditionalTypeReader) {
    if (shouldSkip(field)) {
      return null;
    }

    willResolve(field);
    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(field, value);
    if (value == null) {
      resolveDelegate.didResolveNull();
      didResolve(field);
      return null;
    } else {
      resolveDelegate.didResolveScalar(value);
      didResolve(field);
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

  private void willResolve(ResponseField field) {
    resolveDelegate.willResolve(field, operationVariables);
  }

  private void didResolve(ResponseField field) {
    resolveDelegate.didResolve(field, operationVariables);
  }

  private void checkValue(ResponseField field, Object value) {
    if (!field.optional() && value == null) {
      throw new NullPointerException("corrupted response reader, expected non null value for " + field.fieldName());
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

    @Override public Integer readInt() {
      resolveDelegate.didResolveScalar(value);
      return ((BigDecimal) value).intValue();
    }

    @Override public Long readLong() {
      resolveDelegate.didResolveScalar(value);
      return ((BigDecimal) value).longValue();
    }

    @Override public Double readDouble() {
      resolveDelegate.didResolveScalar(value);
      return ((BigDecimal) value).doubleValue();
    }

    @Override public Boolean readBoolean() {
      resolveDelegate.didResolveScalar(value);
      return (Boolean) value;
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    @Override public <T> T readCustomType(ScalarType scalarType) {
      CustomTypeAdapter<T> typeAdapter = scalarTypeAdapters.adapterFor(scalarType);
      resolveDelegate.didResolveScalar(value);
      return typeAdapter.decode(value.toString());
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T readObject(ObjectReader<T> objectReader) {
      R value = (R) this.value;
      resolveDelegate.willResolveObject(field, Optional.fromNullable(value));
      T item = (T) objectReader.read(new RealResponseReader<R>(operationVariables, value, fieldValueResolver,
          scalarTypeAdapters, resolveDelegate));
      resolveDelegate.didResolveObject(field, Optional.fromNullable(value));
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
        if (value != null) {
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
