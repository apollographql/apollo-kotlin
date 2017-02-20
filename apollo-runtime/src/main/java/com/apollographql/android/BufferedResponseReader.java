package com.apollographql.android;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.ScalarType;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") final class BufferedResponseReader implements ResponseReader {
  private final Map<String, Object> buffer;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

  BufferedResponseReader(Map<String, Object> buffer, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.buffer = buffer;
    this.customTypeAdapters = customTypeAdapters;
  }

  @Override public ResponseReader toBufferedReader() throws IOException {
    return this;
  }

  @Override public void read(ValueHandler handler, Field... fields) throws IOException {
    int fieldIndex = 0;
    for (Field field : fields) {
      switch (field.type()) {
        case STRING:
          handler.handle(fieldIndex, readString(field));
          break;
        case INT:
          handler.handle(fieldIndex, readInt(field));
          break;
        case LONG:
          handler.handle(fieldIndex, readLong(field));
          break;
        case DOUBLE:
          handler.handle(fieldIndex, readDouble(field));
          break;
        case BOOLEAN:
          handler.handle(fieldIndex, readBoolean(field));
          break;
        case OBJECT:
          handler.handle(fieldIndex, readObject((Field.ObjectField) field));
          break;
        case SCALAR_LIST:
          handler.handle(fieldIndex, readScalarList((Field.ScalarListField) field));
          break;
        case OBJECT_LIST:
          handler.handle(fieldIndex, readObjectList((Field.ObjectListField) field));
          break;
        case CUSTOM:
          handler.handle(fieldIndex, readCustomType((Field.CustomTypeField) field));
          break;
        case CONDITIONAL:
          handler.handle(fieldIndex, readConditional((Field.ConditionalTypeField) field));
          break;
        default:
          throw new IllegalArgumentException("Unsupported field type");
      }
      fieldIndex++;
    }
  }

  String readString(Field field) throws IOException {
    String value = (String) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      return null;
    } else {
      return value;
    }
  }

  Integer readInt(Field field) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      return null;
    } else {
      return value.intValue();
    }
  }

  Long readLong(Field field) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      return null;
    } else {
      return value.longValue();
    }
  }

  Double readDouble(Field field) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      return null;
    } else {
      return value.doubleValue();
    }
  }

  Boolean readBoolean(Field field) throws IOException {
    Boolean value = (Boolean) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      return null;
    } else {
      return value;
    }
  }

  @SuppressWarnings("unchecked") <T> T readObject(Field.ObjectField field) throws IOException {
    Map<String, Object> value = (Map<String, Object>) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      return null;
    } else {
      return (T) field.objectReader().read(new BufferedResponseReader(value, customTypeAdapters));
    }
  }

  @SuppressWarnings("unchecked") <T> List<T> readScalarList(Field.ScalarListField field) throws IOException {
    List values = (List) buffer.get(field.responseName());
    checkValue(values, field.optional());
    if (values == null) {
      return null;
    } else {
      List<T> result = new ArrayList<>();
      for (Object value : values) {
        T item = (T) field.listReader().read(new BufferedListItemReader(value, customTypeAdapters));
        result.add(item);
      }
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
      for (Object value : values) {
        T item = (T) field.objectReader().read(new BufferedResponseReader((Map<String, Object>) value,
            customTypeAdapters));
        result.add(item);
      }
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
        return (T) value;
      } else {
        return typeAdapter.decode(value.toString());
      }
    }
  }

  @SuppressWarnings("unchecked") private <T> T readConditional(Field.ConditionalTypeField field) throws IOException {
    String value = (String) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      return null;
    } else {
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