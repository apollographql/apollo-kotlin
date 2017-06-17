package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.internal.field.FieldValueResolver;

import java.io.IOException;
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

  @Override public String readString(Field field) throws IOException {
    willResolve(field);
    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
    } else {
      readerShadow.didParseScalar(value);
    }
    didResolve(field);
    return value;
  }

  @Override public Integer readInt(Field field) throws IOException {
    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
    } else {
      readerShadow.didParseScalar(value);
    }
    didResolve(field);
    return value != null ? value.intValue() : null;
  }

  @Override public Long readLong(Field field) throws IOException {
    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
    } else {
      readerShadow.didParseScalar(value);
    }
    didResolve(field);
    return value != null ? value.longValue() : null;
  }

  @Override public Double readDouble(Field field) throws IOException {
    willResolve(field);
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
    } else {
      readerShadow.didParseScalar(value);
    }
    didResolve(field);
    return value != null ? value.doubleValue() : null;
  }

  @Override public Boolean readBoolean(Field field) throws IOException {
    willResolve(field);
    Boolean value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
    } else {
      readerShadow.didParseScalar(value);
    }
    didResolve(field);
    return value;
  }

  @SuppressWarnings("unchecked") @Override
  public <T> T readObject(Field field, ResponseReader.ObjectReader<T> objectReader)
      throws IOException {
    willResolve(field);
    R value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    readerShadow.willParseObject(field, Optional.fromNullable(value));
    final T parsedValue;
    if (value == null) {
      readerShadow.didParseNull();
      parsedValue = null;
    } else {
      parsedValue = (T) objectReader.read(new RealResponseReader(operationVariables, value, fieldValueResolver,
          customTypeAdapters, readerShadow));
      readerShadow.didParseObject(field, Optional.fromNullable(value));
    }
    didResolve(field);
    return parsedValue;
  }

  @SuppressWarnings("unchecked")
  @Override public <T> List<T> readList(Field field, ListReader listReader) throws IOException {
    willResolve(field);
    List values = fieldValueResolver.valueFor(recordSet, field);
    checkValue(values, field.optional());
    final List<T> result;
    if (values == null) {
      readerShadow.didParseNull();
      result = null;
    } else {
      result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        readerShadow.willParseElement(i);
        Object value = values.get(i);
        T item = (T) listReader.read(new ListItemReader(field, value));
        readerShadow.didParseElement(i);
        result.add(item);
      }
      readerShadow.didParseList(values);
    }
    didResolve(field);
    return result != null ? Collections.unmodifiableList(result) : null;
  }

  @SuppressWarnings("unchecked") @Override public <T> T readCustomType(Field.CustomTypeField field) throws IOException {
    willResolve(field);
    Object value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    final T result;
    if (value == null) {
      readerShadow.didParseNull();
      result = null;
    } else {
      CustomTypeAdapter<T> typeAdapter = customTypeAdapters.get(field.scalarType());
      if (typeAdapter == null) {
        readerShadow.didParseScalar(value);
        result = (T) value;
      } else {
        readerShadow.didParseScalar(value);
        result = typeAdapter.decode(value.toString());
      }
    }
    didResolve(field);
    return result;
  }

  @Override
  public <T> T readConditional(Field.ConditionalTypeField field, ConditionalTypeReader<T> conditionalTypeReader)
      throws IOException {
    willResolve(field);
    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    final T result;
    if (value == null) {
      readerShadow.didParseNull();
      didResolve(field);
      result = null;
    } else if (field.type() == Field.Type.INLINE_FRAGMENT && !field.conditionalTypes().contains(value)) {
      readerShadow.didParseScalar(value);
      didResolve(field);
      result = null;
    } else {
      readerShadow.didParseScalar(value);
      didResolve(field);
      result = (T) conditionalTypeReader.read(value, this);
    }
    return result;
  }

  private void willResolve(Field field) {
    readerShadow.willResolve(field, operationVariables);
  }

  private void didResolve(Field field) {
    readerShadow.didResolve(field, operationVariables);
  }

  private void checkValue(Object value, boolean optional) {
    if (!optional && value == null) {
      throw new NullPointerException("corrupted response reader, expected non null value");
    }
  }

  private class ListItemReader implements ResponseReader.ListItemReader {
    private final Field field;
    private final Object value;

    ListItemReader(Field field, Object value) {
      this.field = field;
      this.value = value;
    }

    @Override public String readString() throws IOException {
      readerShadow.didParseScalar(value);
      return (String) value;
    }

    @Override public Integer readInt() throws IOException {
      readerShadow.didParseScalar(value);
      return ((BigDecimal) value).intValue();
    }

    @Override public Long readLong() throws IOException {
      readerShadow.didParseScalar(value);
      return ((BigDecimal) value).longValue();
    }

    @Override public Double readDouble() throws IOException {
      readerShadow.didParseScalar(value);
      return ((BigDecimal) value).doubleValue();
    }

    @Override public Boolean readBoolean() throws IOException {
      readerShadow.didParseScalar(value);
      return (Boolean) value;
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T readCustomType(ScalarType scalarType) throws IOException {
      CustomTypeAdapter<T> typeAdapter = customTypeAdapters.get(scalarType);
      if (typeAdapter == null) {
        throw new RuntimeException("Can't resolve custom type adapter for " + scalarType.typeName());
      }
      readerShadow.didParseScalar(value);
      return typeAdapter.decode(value.toString());
    }

    @SuppressWarnings("unchecked")
    @Override public <T> T readObject(ObjectReader<T> objectReader) throws IOException {
      R value = (R) this.value;
      readerShadow.willParseObject(field, Optional.fromNullable(value));
      T item = (T) objectReader.read(new RealResponseReader<R>(operationVariables, value, fieldValueResolver,
          customTypeAdapters, readerShadow));
      readerShadow.didParseObject(field, Optional.fromNullable(value));
      return item;
    }
  }
}
