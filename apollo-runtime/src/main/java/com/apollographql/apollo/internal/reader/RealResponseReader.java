package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.ResponseField;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.internal.field.FieldValueResolver;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") public final class RealResponseReader<R> implements ResponseReader {
  private final Operation.Variables operationVariables;
  private final R recordSet;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final FieldValueResolver<R> fieldValueResolver;
  private final ResponseReaderShadow<R> readerShadow;

  public RealResponseReader(Operation.Variables operationVariables, R recordSet,
      FieldValueResolver<R> fieldValueResolver, Map<ScalarType, CustomTypeAdapter> customTypeAdapters,
      ResponseReaderShadow<R> readerShadow) {
    this.operationVariables = operationVariables;
    this.recordSet = recordSet;
    this.fieldValueResolver = fieldValueResolver;
    this.customTypeAdapters = customTypeAdapters;
    this.readerShadow = readerShadow;
  }

  @Override public String readString(ResponseField field) {
    willResolve(field);
    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didResolveNull();
    } else {
      readerShadow.didResolveScalar(value);
    }
    didResolve(field);
    return value;
  }

  @Override public Integer readInt(ResponseField field) {
    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didResolveNull();
    } else {
      readerShadow.didResolveScalar(value);
    }
    didResolve(field);
    return value != null ? value.intValue() : null;
  }

  @Override public Long readLong(ResponseField field) {
    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didResolveNull();
    } else {
      readerShadow.didResolveScalar(value);
    }
    didResolve(field);
    return value != null ? value.longValue() : null;
  }

  @Override public Double readDouble(ResponseField field) {
    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didResolveNull();
    } else {
      readerShadow.didResolveScalar(value);
    }
    didResolve(field);
    return value != null ? value.doubleValue() : null;
  }

  @Override public Boolean readBoolean(ResponseField field) {
    willResolve(field);
    Boolean value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didResolveNull();
    } else {
      readerShadow.didResolveScalar(value);
    }
    didResolve(field);
    return value;
  }

  @SuppressWarnings("unchecked") @Override
  public <T> T readObject(ResponseField field, ResponseReader.ObjectReader<T> objectReader) {
    willResolve(field);
    R value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    readerShadow.willResolveObject(field, Optional.fromNullable(value));
    final T parsedValue;
    if (value == null) {
      readerShadow.didResolveNull();
      parsedValue = null;
    } else {
      parsedValue = (T) objectReader.read(new RealResponseReader(operationVariables, value, fieldValueResolver,
          customTypeAdapters, readerShadow));
    }
    readerShadow.didResolveObject(field, Optional.fromNullable(value));
    didResolve(field);
    return parsedValue;
  }

  @SuppressWarnings("unchecked")
  @Override public <T> List<T> readList(ResponseField field, ListReader listReader) {
    willResolve(field);
    List values = fieldValueResolver.valueFor(recordSet, field);
    checkValue(values, field.optional());
    final List<T> result;
    if (values == null) {
      readerShadow.didResolveNull();
      result = null;
    } else {
      result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        readerShadow.willResolveElement(i);
        Object value = values.get(i);
        if (value != null) {
          T item = (T) listReader.read(new ListItemReader(field, value));
          result.add(item);
        }
        readerShadow.didResolveElement(i);
      }
      readerShadow.didResolveList(values);
    }
    didResolve(field);
    return result != null ? Collections.unmodifiableList(result) : null;
  }

  @SuppressWarnings("unchecked") @Override public <T> T readCustomType(ResponseField.CustomTypeField field) {
    willResolve(field);
    Object value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    final T result;
    if (value == null) {
      readerShadow.didResolveNull();
      result = null;
    } else {
      CustomTypeAdapter<T> typeAdapter = customTypeAdapters.get(field.scalarType());
      if (typeAdapter == null) {
        readerShadow.didResolveScalar(value);
        result = (T) value;
      } else {
        readerShadow.didResolveScalar(value);
        result = typeAdapter.decode(value.toString());
      }
    }
    didResolve(field);
    return result;
  }

  @Override
  public <T> T readConditional(ResponseField field, ConditionalTypeReader<T> conditionalTypeReader) {
    willResolve(field);
    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didResolveNull();
      didResolve(field);
      return null;
    } else {
      readerShadow.didResolveScalar(value);
      didResolve(field);
      if (field.type() == ResponseField.Type.INLINE_FRAGMENT) {
        for (ResponseField.Condition condition : field.conditions()) {
          if (condition instanceof ResponseField.TypeNameCondition) {
            if (((ResponseField.TypeNameCondition) condition).typeName().equals(value)) {
              return (T) conditionalTypeReader.read(value, this);
            }
          }
        }
        return null;
      } else {
        return (T) conditionalTypeReader.read(value, this);
      }
    }
  }

  private void willResolve(ResponseField field) {
    readerShadow.willResolve(field, operationVariables);
  }

  private void didResolve(ResponseField field) {
    readerShadow.didResolve(field, operationVariables);
  }

  private void checkValue(Object value, boolean optional) {
    if (!optional && value == null) {
      throw new NullPointerException("corrupted response reader, expected non null value");
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
      readerShadow.didResolveScalar(value);
      return (String) value;
    }

    @Override public Integer readInt() {
      readerShadow.didResolveScalar(value);
      return ((BigDecimal) value).intValue();
    }

    @Override public Long readLong() {
      readerShadow.didResolveScalar(value);
      return ((BigDecimal) value).longValue();
    }

    @Override public Double readDouble() {
      readerShadow.didResolveScalar(value);
      return ((BigDecimal) value).doubleValue();
    }

    @Override public Boolean readBoolean() {
      readerShadow.didResolveScalar(value);
      return (Boolean) value;
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T readCustomType(ScalarType scalarType) {
      CustomTypeAdapter<T> typeAdapter = customTypeAdapters.get(scalarType);
      if (typeAdapter == null) {
        throw new RuntimeException("Can't resolve custom type adapter for " + scalarType.typeName());
      }
      readerShadow.didResolveScalar(value);
      return typeAdapter.decode(value.toString());
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T readObject(ObjectReader<T> objectReader) {
      R value = (R) this.value;
      readerShadow.willResolveObject(field, Optional.fromNullable(value));
      T item = (T) objectReader.read(new RealResponseReader<R>(operationVariables, value, fieldValueResolver,
          customTypeAdapters, readerShadow));
      readerShadow.didResolveObject(field, Optional.fromNullable(value));
      return item;
    }
  }
}
