package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.ResponseReader;

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
        case Field.TYPE_STRING:
          handler.handle(fieldIndex, readString(field));
          break;
        case Field.TYPE_INT:
          handler.handle(fieldIndex, readInt(field));
          break;
        case Field.TYPE_LONG:
          handler.handle(fieldIndex, readLong(field));
          break;
        case Field.TYPE_DOUBLE:
          handler.handle(fieldIndex, readDouble(field));
          break;
        case Field.TYPE_BOOL:
          handler.handle(fieldIndex, readBoolean(field));
          break;
        case Field.TYPE_OBJECT:
          handler.handle(fieldIndex, readObject(field));
          break;
        case Field.TYPE_LIST:
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
    if (value == null) {
      if (field.optional()) {
        return null;
      } else {
        throw new NullPointerException("can't parse response, expected non null json value");
      }
    } else {
      return value;
    }
  }

  Integer readInt(Field field) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    if (value == null) {
      if (field.optional()) {
        return null;
      } else {
        throw new NullPointerException("can't parse response, expected non null json value");
      }
    } else {
      return value.intValue();
    }
  }

  Long readLong(Field field) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    if (value == null) {
      if (field.optional()) {
        return null;
      } else {
        throw new NullPointerException("can't parse response, expected non null json value");
      }
    } else {
      return value.longValue();
    }
  }

  Double readDouble(Field field) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(field.responseName());
    if (value == null) {
      if (field.optional()) {
        return null;
      } else {
        throw new NullPointerException("can't parse response, expected non null json value");
      }
    } else {
      return value.doubleValue();
    }
  }

  Boolean readBoolean(Field field) throws IOException {
    Boolean value = (Boolean) buffer.get(field.responseName());
    if (value == null) {
      if (field.optional()) {
        return null;
      } else {
        throw new NullPointerException("can't parse response, expected non null json value");
      }
    } else {
      return value;
    }
  }

  @SuppressWarnings("unchecked") <T> T readObject(Field field) throws IOException {
    final Map<String, Object> value = (Map<String, Object>) buffer.get(field.responseName());
    if (value == null) {
      if (field.optional()) {
        return null;
      } else {
        throw new NullPointerException("can't parse response, expected non null json value");
      }
    } else {
      return (T) field.nestedReader().read(new BufferedResponseReader(value));
    }
  }

  @SuppressWarnings("unchecked") <T> List<T> readList(Field field) throws IOException {
    final List<Map<String, Object>> value = (List<Map<String, Object>>) buffer.get(field.responseName());
    if (value == null) {
      if (field.optional()) {
        return null;
      } else {
        throw new NullPointerException("can't parse response, expected non null json value");
      }
    } else {
      List<T> result = new ArrayList<>();
      for (Map<String, Object> map : value) {
        result.add((T) field.nestedReader().read(new BufferedResponseReader(map)));
      }
      return result;
    }
  }
}
