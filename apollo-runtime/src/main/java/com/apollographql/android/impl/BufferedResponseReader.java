package com.apollographql.android.impl;

import com.apollographql.android.CustomTypeAdapter;
import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.ScalarType;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") final class BufferedResponseReader implements ResponseReader {
  private final Map<String, Object> buffer;
  private final Operation operation;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final ResponseReaderShadow readerShadow;

  BufferedResponseReader(Map<String, Object> buffer, Operation operation,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, ResponseReaderShadow readerShadow) {
    this.buffer = buffer;
    this.operation = operation;
    this.customTypeAdapters = customTypeAdapters;
    this.readerShadow = readerShadow;
  }

  @Override public void read(ValueHandler handler, Field... fields) throws IOException {
    int fieldIndex = 0;
    for (Field field : fields) {
      Object value = read(field);
      handler.handle(fieldIndex, value);
      fieldIndex++;
    }
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

  @Override public Operation operation() {
    return operation;
  }

  String readString(Field field) throws IOException {
    String value = (String) buffer.get(field.responseName());
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
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      Integer intValue = value.intValue();
      readerShadow.didParseScalar(value.intValue());
      return intValue;
    }
  }

  Long readLong(Field field) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      Long longValue = value.longValue();
      readerShadow.didParseScalar(longValue);
      return longValue;
    }
  }

  Double readDouble(Field field) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    checkValue(value, field.optional());
    readerShadow.didParseScalar(value);
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      Double doubleValue = value.doubleValue();
      readerShadow.didParseScalar(doubleValue);
      return doubleValue;
    }
  }

  Boolean readBoolean(Field field) throws IOException {
    Boolean value = (Boolean) buffer.get(field.responseName());
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
    Map<String, Object> value = (Map<String, Object>) buffer.get(field.responseName());
    checkValue(value, field.optional());
    readerShadow.willParseObject(value);
    if (value == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      final T parsedValue = (T) field.objectReader().read(new BufferedResponseReader(value, operation,
          customTypeAdapters, readerShadow));
      readerShadow.didParseObject(value);
      return parsedValue;
    }
  }

  @SuppressWarnings("unchecked") <T> List<T> readScalarList(Field.ScalarListField field) throws IOException {
    List values = (List) buffer.get(field.responseName());
    checkValue(values, field.optional());
    if (values == null) {
      readerShadow.didParseNull();
      return null;
    } else {
      List<T> result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        readerShadow.willParseElement(i);
        Object value = values.get(i);
        T item = (T) field.listReader().read(new BufferedListItemReader(value, customTypeAdapters));
        readerShadow.didParseScalar(value);
        readerShadow.didParseElement(i);
        result.add(item);
      }
      readerShadow.didParseList(values);
      return result;
    }
  }

  @SuppressWarnings("unchecked") <T> List<T> readObjectList(Field.ObjectListField field) throws IOException {
    List values = (List) buffer.get(field.responseName());
    checkValue(values, field.optional());
    if (values == null) {
      return null;
    } else {
      List<T> result = new ArrayList<>();
      for (int i = 0; i < values.size(); i++) {
        readerShadow.willParseElement(i);
        Object value = values.get(i);
        final Map<String, Object> objectMap = (Map<String, Object>) value;
        readerShadow.willParseObject(objectMap);
        T item = (T) field.objectReader().read(new BufferedResponseReader(objectMap, operation,
            customTypeAdapters, readerShadow));
        readerShadow.didParseObject(objectMap);
        readerShadow.didParseElement(i);
        result.add(item);
      }
      readerShadow.didParseList(values);
      return result;
    }
  }

  @SuppressWarnings("unchecked") private <T> T readCustomType(Field.CustomTypeField field) {
    Object value = buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
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
      Operation.Variables variables) throws
      IOException {
    readerShadow.willResolve(field, variables);
    String value = (String) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      readerShadow.didParseNull();
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

  private static class BufferedListItemReader implements Field.ListItemReader {
    private final Object value;
    private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

    BufferedListItemReader(Object value, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
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
