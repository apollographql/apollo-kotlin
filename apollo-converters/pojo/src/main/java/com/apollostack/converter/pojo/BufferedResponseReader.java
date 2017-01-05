package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.ResponseReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BufferedResponseReader implements ResponseReader {
  private final Map<String, Object> buffer;

  BufferedResponseReader(Map<String, Object> buffer) {
    this.buffer = buffer;
  }

  @Override public String readString(String responseName, String fieldName, Map<String, Object> arguments) throws
      IOException {
    String value = (String) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value;
  }

  @Override public String readOptionalString(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return (String) buffer.get(responseName);
  }

  @Override public int readInt(String responseName, String fieldName, Map<String, Object> arguments) throws
      IOException {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value.intValue();
  }

  @Override public Integer readOptionalInt(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      return null;
    }
    return value.intValue();
  }

  @Override
  public long readLong(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value.longValue();
  }

  @Override
  public Long readOptionalLong(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      return null;
    }
    return value.longValue();
  }

  @Override public double readDouble(String responseName, String fieldName, Map<String, Object> arguments) throws
      IOException {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value.doubleValue();
  }

  @Override public Double readOptionalDouble(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      return null;
    }
    return value.doubleValue();
  }

  @Override public boolean readBoolean(String responseName, String fieldName, Map<String, Object> arguments) throws
      IOException {
    Boolean value = (Boolean) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value;
  }

  @Override public Boolean readOptionalBoolean(String responseName, String fieldName, Map<String, Object> arguments)
      throws IOException {
    return (Boolean) buffer.get(responseName);
  }

  @Override
  public <T> T readObject(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T>
      reader) throws IOException {
    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) buffer.get(responseName);
    if (map == null) {
      throw new NullPointerException("can't parse response, expected non null value");
    }
    return reader.read(new BufferedResponseReader(map));
  }

  @Override
  public <T> T readOptionalObject(String responseName, String fieldName, Map<String, Object> arguments,
      NestedReader<T> reader) throws IOException {
    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) buffer.get(responseName);
    if (map == null) {
      return null;
    }
    return reader.read(new BufferedResponseReader(map));
  }

  @Override
  public <T> List<T> readList(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T>
      reader) throws IOException {
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> list = (List<Map<String, Object>>) buffer.get(responseName);
    if (list == null) {
      throw new NullPointerException("can't parse response, expected non null value");
    }

    List<T> result = new ArrayList<>();
    for (Map<String, Object> map : list) {
      result.add(reader.read(new BufferedResponseReader(map)));
    }
    return result;
  }

  @Override
  public <T> List<T> readOptionalList(String responseName, String fieldName, Map<String, Object> arguments,
      NestedReader<T> reader) throws IOException {
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> list = (List<Map<String, Object>>) buffer.get(responseName);
    if (list == null) {
      return null;
    }

    List<T> result = new ArrayList<>();
    for (Map<String, Object> map : list) {
      result.add(reader.read(new BufferedResponseReader(map)));
    }
    return result;
  }
}
