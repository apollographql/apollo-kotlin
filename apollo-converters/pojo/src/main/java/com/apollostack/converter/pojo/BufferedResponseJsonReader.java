package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.BufferedResponseReader;
import com.apollostack.api.graphql.ResponseStreamReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class BufferedResponseJsonReader implements BufferedResponseReader {
  private final Map<String, Object> buffer;

  static BufferedResponseJsonReader fromStreamReader(ResponseStreamReader streamReader) throws IOException {
    Map<String, Object> buffer = toMap(streamReader);
    return new BufferedResponseJsonReader(buffer);
  }

  private BufferedResponseJsonReader(Map<String, Object> buffer) {
    this.buffer = buffer;
  }

  @Override public String readString(String responseName, String fieldName) {
    String value = (String) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value;
  }

  @Override public String readOptionalString(String responseName, String fieldName) {
    return (String) buffer.get(responseName);
  }

  @Override public int readInt(String responseName, String fieldName) {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value.intValue();
  }

  @Override public Integer readOptionalInt(String responseName, String fieldName) {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      return null;
    }
    return value.intValue();
  }

  @Override public double readDouble(String responseName, String fieldName) {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value.doubleValue();
  }

  @Override public Double readOptionalDouble(String responseName, String fieldName) {
    BigDecimal value = (BigDecimal) buffer.get(responseName);
    if (value == null) {
      return null;
    }
    return value.doubleValue();
  }

  @Override public boolean readBoolean(String responseName, String fieldName) {
    Boolean value = (Boolean) buffer.get(responseName);
    if (value == null) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return value;
  }

  @Override public Boolean readOptionalBoolean(String responseName, String fieldName) {
    return (Boolean) buffer.get(responseName);
  }

  @Override public <T> T readObject(String responseName, String fieldName, NestedReader<T> reader) {
    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) buffer.get(responseName);
    if (map == null) {
      throw new NullPointerException("can't parse response, expected non null value");
    }
    return reader.read(new BufferedResponseJsonReader(map));
  }

  @Override public <T> T readOptionalObject(String responseName, String fieldName, NestedReader<T> reader) {
    @SuppressWarnings("unchecked")
    final Map<String, Object> map = (Map<String, Object>) buffer.get(responseName);
    if (map == null) {
      return null;
    }
    return reader.read(new BufferedResponseJsonReader(map));
  }

  @Override public <T> List<T> readList(String responseName, String fieldName, NestedReader<T> reader) {
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> list = (List<Map<String, Object>>) buffer.get(responseName);
    if (list == null) {
      throw new NullPointerException("can't parse response, expected non null value");
    }

    List<T> result = new ArrayList<>();
    for (Map<String, Object> map : list) {
      result.add(reader.read(new BufferedResponseJsonReader(map)));
    }
    return result;
  }

  @Override public <T> List<T> readOptionalList(String responseName, String fieldName, NestedReader<T> reader) {
    @SuppressWarnings("unchecked")
    final List<Map<String, Object>> list = (List<Map<String, Object>>) buffer.get(responseName);
    if (list == null) {
      return null;
    }

    List<T> result = new ArrayList<>();
    for (Map<String, Object> map : list) {
      result.add(reader.read(new BufferedResponseJsonReader(map)));
    }
    return result;
  }

  @Override public <T> List<T> readList(String responseName, String fieldName, Converter<T> converter) {
    @SuppressWarnings("unchecked")
    final List<Object> list = (List<Object>) buffer.get(responseName);
    if (list == null) {
      throw new NullPointerException("can't parse response, expected non null value");
    }

    List<T> result = new ArrayList<>();
    for (Object item : list) {
      result.add(converter.convert(item));
    }
    return result;
  }

  @Override public <T> List<T> readOptionalList(String responseName, String fieldName, Converter<T> converter) {
    @SuppressWarnings("unchecked")
    final List<Object> list = (List<Object>) buffer.get(responseName);
    if (list == null) {
      return null;
    }

    List<T> result = new ArrayList<>();
    for (Object item : list) {
      result.add(converter.convert(item));
    }
    return result;
  }

  private static Map<String, Object> toMap(ResponseStreamReader streamReader) throws IOException {
    Map<String, Object> result = new HashMap<>();
    while (streamReader.hasNext()) {
      String name = streamReader.nextName();
      if (streamReader.isNextNull()) {
        streamReader.skipNext();
      } else if (streamReader.isNextObject()) {
        result.put(name, readObject(streamReader));
      } else if (streamReader.isNextList()) {
        result.put(name, readList(streamReader));
      } else {
        result.put(name, readScalar(streamReader));
      }
    }
    return result;
  }

  private static Map<String, Object> readObject(final ResponseStreamReader streamReader) throws IOException {
    return streamReader.nextObject(new ResponseStreamReader.NestedReader<Map<String, Object>>() {
      @Override public Map<String, Object> read(ResponseStreamReader reader) throws IOException {
        return toMap(streamReader);
      }
    });
  }

  private static List<?> readList(final ResponseStreamReader streamReader) throws IOException {
    return streamReader.nextList(new ResponseStreamReader.NestedReader() {
      @Override public Object read(ResponseStreamReader reader) throws IOException {
        if (reader.isNextObject()) {
          return readObject(streamReader);
        } else if (reader.isNextList()) {
          return readList(streamReader);
        } else {
          return readScalar(streamReader);
        }
      }
    });
  }

  private static Object readScalar(ResponseStreamReader streamReader) throws IOException {
    if (streamReader.isNextNull()) {
      streamReader.skipNext();
      return null;
    } else if (streamReader.isNextBoolean()) {
      return streamReader.nextBoolean();
    } else if (streamReader.isNextNumber()) {
      return new BigDecimal(streamReader.nextString());
    } else {
      return streamReader.nextString();
    }
  }
}
