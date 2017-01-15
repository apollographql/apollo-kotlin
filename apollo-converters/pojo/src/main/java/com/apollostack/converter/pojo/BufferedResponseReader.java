package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.ResponseReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
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

  @Override public String readString() throws IOException {
    checkSingleValue();

    String value = (String) buffer.get("");
    checkValue(value, false);

    return value;
  }

  @Override public Integer readInt() throws IOException {
    checkSingleValue();

    BigDecimal value = (BigDecimal) buffer.get("");
    checkValue(value, false);

    return value.intValue();
  }

  @Override public Long readLong() throws IOException {
    checkSingleValue();

    BigDecimal value = (BigDecimal) buffer.get("");
    checkValue(value, false);

    return value.longValue();
  }

  @Override public Double readDouble() throws IOException {
    checkSingleValue();

    BigDecimal value = (BigDecimal) buffer.get("");
    checkValue(value, false);

    return value.doubleValue();
  }

  @Override public Boolean readBoolean() throws IOException {
    checkSingleValue();

    Boolean value = (Boolean) buffer.get("");
    checkValue(value, false);

    return value;
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
      return (T) field.nestedReader().read(new BufferedResponseReader(value));
    }
  }

  @SuppressWarnings("unchecked") <T> List<T> readList(Field field) throws IOException {
    List<Map<String, Object>> values = (List<Map<String, Object>>) buffer.get(field.responseName());
    checkValue(values, field.optional());
    List<T> result = new ArrayList<>();
    for (Map<String, Object> value : values) {
      result.add((T) field.nestedReader().read(new BufferedResponseReader(value)));
    }
    return result;
  }

  @SuppressWarnings("unchecked") private void checkSingleValue() {
    if (buffer.size() != 1) {
      throw new IllegalStateException("corrupted response reader, expected single value");
    }
  }

  private void checkValue(Object value, boolean optional) {
    if (!optional && value == null) {
      throw new NullPointerException("corrupted response reader, expected non null value");
    }
  }
}
