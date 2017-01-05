package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.ResponseReader;
import com.apollostack.api.graphql.ResponseStreamReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ResponseJsonStreamReader implements ResponseStreamReader {
  private final JsonReader jsonReader;

  ResponseJsonStreamReader(JsonReader jsonReader) {
    this.jsonReader = jsonReader;
  }

  @Override public boolean hasNext() throws IOException {
    return jsonReader.hasNext();
  }

  @Override public String nextName() throws IOException {
    return jsonReader.nextName();
  }

  @Override public void skipNext() throws IOException {
    jsonReader.skipValue();
  }

  @Override public boolean isNextObject() throws IOException {
    return jsonReader.peek() == JsonReader.Token.BEGIN_OBJECT;
  }

  @Override public boolean isNextList() throws IOException {
    return jsonReader.peek() == JsonReader.Token.BEGIN_ARRAY;
  }

  @Override public boolean isNextNull() throws IOException {
    return jsonReader.peek() == JsonReader.Token.NULL;
  }

  @Override public boolean isNextBoolean() throws IOException {
    return jsonReader.peek() == JsonReader.Token.BOOLEAN;
  }

  @Override public boolean isNextNumber() throws IOException {
    return jsonReader.peek() == JsonReader.Token.NUMBER;
  }

  @Override
  public String readString(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextString();
  }

  @Override
  public String readOptionalString(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextOptionalString();
  }

  @Override
  public int readInt(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextInt();
  }

  @Override
  public Integer readOptionalInt(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextOptionalInt();
  }

  @Override
  public long readLong(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextLong();
  }

  @Override
  public Long readOptionalLong(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextOptionalLong();
  }

  @Override
  public double readDouble(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextDouble();
  }

  @Override
  public Double readOptionalDouble(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextOptionalDouble();
  }

  @Override
  public boolean readBoolean(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextBoolean();
  }

  @Override
  public Boolean readOptionalBoolean(String responseName, String fieldName, Map<String, Object> arguments) throws IOException {
    return nextOptionalBoolean();
  }

  @Override
  public <T> T readObject(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T> reader) throws IOException {
    return nextOptionalObject(reader);
  }

  @Override
  public <T> T readOptionalObject(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T> reader) throws IOException {
    return nextOptionalObject(reader);
  }

  @Override
  public <T> List<T> readList(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T> reader) throws IOException {
    return nextList(reader);
  }

  @Override
  public <T> List<T> readOptionalList(String responseName, String fieldName, Map<String, Object> arguments, NestedReader<T> reader) throws IOException {
    return nexOptionalList(reader);
  }

  @Override public ResponseReader buffer() throws IOException {
    Map<String, Object> buffer = toMap(this);
    return new BufferedResponseReader(buffer);
  }

  String nextString() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextString();
  }

  String nextOptionalString() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextString();
  }

  int nextInt() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextInt();
  }

  Integer nextOptionalInt() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextInt();
  }

  long nextLong() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextLong();
  }

  Long nextOptionalLong() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextLong();
  }

  double nextDouble() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextDouble();
  }

  Double nextOptionalDouble() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextDouble();
  }

  boolean nextBoolean() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextBoolean();
  }

  Boolean nextOptionalBoolean() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextBoolean();
  }

  <T> T nextObject(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return nextOptionalObject(nestedReader);
  }

  <T> T nextOptionalObject(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }

    jsonReader.beginObject();
    T result = nestedReader.read(this);
    jsonReader.endObject();
    return result;
  }

  <T> List<T> nextList(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return nexOptionalList(nestedReader);
  }

  <T> List<T> nexOptionalList(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }

    List<T> result = new ArrayList<>();
    jsonReader.beginArray();
    while (jsonReader.hasNext()) {
      T item;
      if (isNextObject()) {
        item = nextObject(nestedReader);
      } else {
        item = nestedReader.read(this);
      }
      result.add(item);
    }
    jsonReader.endArray();
    return result;
  }

  private static Map<String, Object> toMap(ResponseJsonStreamReader streamReader) throws IOException {
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

  private static Map<String, Object> readObject(final ResponseJsonStreamReader streamReader) throws IOException {
    return streamReader.nextObject(new NestedReader<Map<String, Object>>() {
      @Override public Map<String, Object> read(ResponseReader streamReader) throws IOException {
        return toMap((ResponseJsonStreamReader) streamReader);
      }
    });
  }

  private static List<?> readList(final ResponseJsonStreamReader streamReader) throws IOException {
    return streamReader.nextList(new NestedReader() {
      @Override public Object read(ResponseReader reader) throws IOException {
        ResponseJsonStreamReader streamReader = (ResponseJsonStreamReader) reader;
        if (streamReader.isNextObject()) {
          return readObject(streamReader);
        } else if (streamReader.isNextList()) {
          return readList(streamReader);
        } else {
          return readScalar(streamReader);
        }
      }
    });
  }

  private static Object readScalar(ResponseJsonStreamReader streamReader) throws IOException {
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
