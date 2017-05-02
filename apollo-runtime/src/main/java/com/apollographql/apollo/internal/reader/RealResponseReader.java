package com.apollographql.apollo.internal.reader;

import com.apollographql.apollo.api.Field;
import com.apollographql.apollo.api.Operation;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.api.ScalarType;
import com.apollographql.apollo.CustomTypeAdapter;
import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.internal.field.FieldValueResolver;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") public final class RealResponseReader<R> implements ResponseReader {
  private final Operation operation;
  private final R recordSet;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final FieldValueResolver<R> fieldValueResolver;
  private final ResponseReaderShadow<R> readerShadow;

  public RealResponseReader(Operation operation, R recordSet, FieldValueResolver<R> fieldValueResolver,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, ResponseReaderShadow<R> readerShadow) {
    this.operation = operation;
    this.recordSet = recordSet;
    this.fieldValueResolver = fieldValueResolver;
    this.customTypeAdapters = customTypeAdapters;
    this.readerShadow = readerShadow;
  }

  @Override public <T> T read(Field field) throws IOException {
    final Object value;
    willResolve(field);
    switch (field.type()) {
      case STRING:
        value = readString(field);
        break;
      case INT:
        value = readInt(field);
        break;
      case LONG:
        value = readLong(field);
        break;
      case DOUBLE:
        value = readDouble(field);
        break;
      case BOOLEAN:
        value = readBoolean(field);
        break;
      case OBJECT:
        value = readObject((Field.ObjectField) field);
        break;
      case SCALAR_LIST:
        value = readScalarList((Field.ScalarListField) field);
        break;
      case OBJECT_LIST:
        value = readObjectList((Field.ObjectListField) field);
        break;
      case CUSTOM:
        value = readCustomType((Field.CustomTypeField) field);
        break;
      case CONDITIONAL:
        value = readConditional((Field.ConditionalTypeField) field, operation.variables());
        break;
      default:
        throw new IllegalArgumentException("Unsupported field type");
    }
    didResolve(field);
    //noinspection unchecked
    return (T) value;
  }

  private void willResolve(Field field) {
    if (field.type() != Field.Type.CONDITIONAL) {
      readerShadow.willResolve(field, operation.variables());
    }
  }

  private void didResolve(Field field) {
    if (field.type() != Field.Type.CONDITIONAL) {
      readerShadow.didResolve(field, operation.variables());
    }
  }

  String readString(Field field) throws IOException {
    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      readerShadow.didParseScalar(value);
      return value;
    }
  }

  Integer readInt(Field field) throws IOException {
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      readerShadow.didParseScalar(value);
      return value.intValue();
    }
  }

  Long readLong(Field field) throws IOException {
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      readerShadow.didParseScalar(value);
      return value.longValue();
    }
  }

  Double readDouble(Field field) throws IOException {
    BigDecimal value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      readerShadow.didParseScalar(value);
      return value.doubleValue();
    }
  }

  Boolean readBoolean(Field field) throws IOException {
    Boolean value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      readerShadow.didParseScalar(value);
      return value;
    }
  }

  @SuppressWarnings("unchecked") <T> T readObject(Field.ObjectField field) throws IOException {
    R value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    readerShadow.willParseObject(field, Optional.fromNullable(value));
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      final T parsedValue = (T) field.objectReader().read(new RealResponseReader(operation, value,
          fieldValueResolver, customTypeAdapters, readerShadow));
      readerShadow.didParseObject(field, Optional.fromNullable(value));
      return parsedValue;
    }
  }

  @SuppressWarnings("unchecked") <T> List<T> readScalarList(Field.ScalarListField field) throws IOException {
    List values = fieldValueResolver.valueFor(recordSet, field);
    checkValue(values, field.optional());
    if (values == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      List<T> result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        readerShadow.willParseElement(i);
        Object value = values.get(i);
        T item = (T) field.listReader().read(new ListItemReader(value, customTypeAdapters));
        readerShadow.didParseScalar(value);
        readerShadow.didParseElement(i);
        result.add(item);
      }
      readerShadow.didParseList(values);
      return Collections.unmodifiableList(result);
    }
  }

  @SuppressWarnings("unchecked") <T> List<T> readObjectList(Field.ObjectListField field) throws IOException {
    List<R> values = fieldValueResolver.valueFor(recordSet, field);
    checkValue(values, field.optional());
    if (values == null) {
      return null;
    } else {
      List<T> result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        readerShadow.willParseElement(i);
        R value = values.get(i);
        readerShadow.willParseObject(field, Optional.fromNullable(value));
        T item = (T) field.objectReader().read(new RealResponseReader(operation, value, fieldValueResolver,
            customTypeAdapters, readerShadow));
        readerShadow.didParseObject(field, Optional.fromNullable(value));
        readerShadow.didParseElement(i);
        result.add(item);
      }
      readerShadow.didParseList(values);
      return Collections.unmodifiableList(result);
    }
  }

  @SuppressWarnings("unchecked") private <T> T readCustomType(Field.CustomTypeField field) {
    Object value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      CustomTypeAdapter<T> typeAdapter = customTypeAdapters.get(field.scalarType());
      if (typeAdapter == null) {
        readerShadow.didParseScalar(value);
        return (T) value;
      } else {
        readerShadow.didParseScalar(value);
        return typeAdapter.decode(value.toString());
      }
    }
  }

  @SuppressWarnings("unchecked") private <T> T readConditional(Field.ConditionalTypeField field,
      Operation.Variables variables) throws IOException {
    readerShadow.willResolve(field, variables);
    String value = fieldValueResolver.valueFor(recordSet, field);
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      readerShadow.didResolve(field, variables);
      return null;
    } else {
      readerShadow.didParseScalar(value);
      readerShadow.didResolve(field, variables);
      return (T) field.conditionalTypeReader().read(value, this);
    }
  }

  private void checkValue(Object value, boolean optional) {
    if (!optional && value == null) {
      throw new NullPointerException("corrupted response reader, expected non null value");
    }
  }

  private static class ListItemReader implements Field.ListItemReader {
    private final Object value;
    private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

    ListItemReader(Object value, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
      this.value = value;
      this.customTypeAdapters = customTypeAdapters;
    }

    @Override public String readString() throws IOException {

      return (String) value;
    }

    @Override public Integer readInt() throws IOException {
      return ((BigDecimal) value).intValue();
    }

    @Override public Long readLong() throws IOException {
      return ((BigDecimal) value).longValue();
    }

    @Override public Double readDouble() throws IOException {
      return ((BigDecimal) value).doubleValue();
    }

    @Override public Boolean readBoolean() throws IOException {
      return (Boolean) value;
    }

    @SuppressWarnings("unchecked") @Override public <T> T readCustomType(ScalarType scalarType) throws IOException {
      CustomTypeAdapter<T> typeAdapter = customTypeAdapters.get(scalarType);
      if (typeAdapter == null) {
        throw new RuntimeException("Can't resolve custom type adapter for " + scalarType.typeName());
      }
      return typeAdapter.decode(value.toString());
    }
  }
}
