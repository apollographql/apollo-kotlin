package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") final class BufferedResponseReader implements ResponseReader {
  private final Map<String, Object> buffer;

  BufferedResponseReader(Map<String, Object> buffer) {
    this.buffer = buffer;
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
          handler.handle(fieldIndex, readObject(field));
          break;
        case LIST:
          handler.handle(fieldIndex, readList(field));
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

  @SuppressWarnings("unchecked") <T> T readObject(Field field) throws IOException {
    Map<String, Object> value = (Map<String, Object>) buffer.get(field.responseName());
    checkValue(value, field.optional());
    if (value == null) {
      return null;
    } else {
      return (T) field.objectReader().read(new BufferedResponseReader(value));
    }
  }

  @SuppressWarnings("unchecked") <T> List<T> readList(Field field) throws IOException {
    List values = (List) buffer.get(field.responseName());
    checkValue(values, field.optional());
    if (values == null) {
      return null;
    } else {
      List<T> result = new ArrayList<>();
      for (Object value : values) {
        T item;
        if (value instanceof Map) {
          item = (T) field.objectReader().read(new BufferedResponseReader((Map<String, Object>) value));
        } else {
          item = (T) field.listReader().read(new BufferedListItemReader(value));
        }
        result.add(item);
      }
      return result;
    }
  }

  private void checkValue(Object value, boolean optional) {
    if (!optional && value == null) {
      throw new NullPointerException("corrupted response reader, expected non null value");
    }
  }

  private static class BufferedListItemReader implements Field.ListItemReader {
    private final Object value;

    BufferedListItemReader(Object value) {
      this.value = value;
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
  }
}