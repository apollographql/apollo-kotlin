package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Field;
import com.apollostack.api.graphql.ResponseReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") final class ResponseJsonStreamReader implements ResponseReader {
  private final JsonReader jsonReader;

  ResponseJsonStreamReader(JsonReader jsonReader) {
    this.jsonReader = jsonReader;
  }

  @Override public ResponseReader toBufferedReader() throws IOException {
    Map<String, Object> buffer = toMap(this);
    return new BufferedResponseReader(buffer);
  }

  @Override public void read(ValueHandler handler, Field... fields) throws IOException {
    Map<String, Integer> fieldIndexMap = new HashMap<>();
    Map<String, Field> fieldMap = new HashMap<>();
    int index = 0;
    for (Field field : fields) {
      fieldMap.put(field.responseName(), field);
      fieldIndexMap.put(field.responseName(), index++);
    }

    while (hasNext()) {
      String nextName = nextName();
      Field field = fieldMap.get(nextName);
      if (field != null) {
        int fieldIndex = fieldIndexMap.get(nextName);
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
      } else {
        skipNext();
      }
    }
  }

  boolean hasNext() throws IOException {
    return jsonReader.hasNext();
  }

  String nextName() throws IOException {
    return jsonReader.nextName();
  }

  void skipNext() throws IOException {
    jsonReader.skipValue();
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

  <T> T nextObject(Field.NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return nextOptionalObject(nestedReader);
  }

  <T> T nextOptionalObject(Field.NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }

    jsonReader.beginObject();
    T result = nestedReader.read(this);
    jsonReader.endObject();
    return result;
  }

  <T> List<T> nextList(Field.NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return nexOptionalList(nestedReader);
  }

  <T> List<T> nexOptionalList(Field.NestedReader<T> nestedReader) throws IOException {
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

  private String readString(Field field) throws IOException {
    if (field.optional()) {
      return nextOptionalString();
    } else {
      return nextString();
    }
  }

  private Integer readInt(Field field) throws IOException {
    if (field.optional()) {
      return nextOptionalInt();
    } else {
      return nextInt();
    }
  }

  private Long readLong(Field field) throws IOException {
    if (field.optional()) {
      return nextOptionalLong();
    } else {
      return nextLong();
    }
  }

  private Double readDouble(Field field) throws IOException {
    if (field.optional()) {
      return nextOptionalDouble();
    } else {
      return nextDouble();
    }
  }

  private Boolean readBoolean(Field field) throws IOException {
    if (field.optional()) {
      return nextOptionalBoolean();
    } else {
      return nextBoolean();
    }
  }

  @SuppressWarnings("unchecked") private <T> T readObject(Field field) throws IOException {
    if (field.optional()) {
      return (T) nextOptionalObject(field.nestedReader());
    } else {
      return (T) nextOptionalObject(field.nestedReader());
    }
  }

  @SuppressWarnings("unchecked") private <T> List<T> readList(Field field) throws IOException {
    if (field.optional()) {
      return nexOptionalList(field.nestedReader());
    } else {
      return nextList(field.nestedReader());
    }
  }

  private boolean isNextObject() throws IOException {
    return jsonReader.peek() == JsonReader.Token.BEGIN_OBJECT;
  }

  private boolean isNextList() throws IOException {
    return jsonReader.peek() == JsonReader.Token.BEGIN_ARRAY;
  }

  private boolean isNextNull() throws IOException {
    return jsonReader.peek() == JsonReader.Token.NULL;
  }

  private boolean isNextBoolean() throws IOException {
    return jsonReader.peek() == JsonReader.Token.BOOLEAN;
  }

  private boolean isNextNumber() throws IOException {
    return jsonReader.peek() == JsonReader.Token.NUMBER;
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
    return streamReader.nextObject(new Field.NestedReader<Map<String, Object>>() {
      @Override public Map<String, Object> read(ResponseReader streamReader) throws IOException {
        return toMap((ResponseJsonStreamReader) streamReader);
      }
    });
  }

  private static List<?> readList(final ResponseJsonStreamReader streamReader) throws IOException {
    return streamReader.nextList(new Field.NestedReader<Object>() {
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
