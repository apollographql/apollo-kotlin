package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.Field;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.api.graphql.TypeMapping;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("WeakerAccess") final class ResponseJsonStreamReader implements ResponseReader {
  private final JsonReader jsonReader;
  private final Map<TypeMapping, CustomTypeAdapter> customTypeAdapters;

  ResponseJsonStreamReader(JsonReader jsonReader, Map<TypeMapping, CustomTypeAdapter> customTypeAdapters) {
    this.jsonReader = jsonReader;
    this.customTypeAdapters = customTypeAdapters;
  }

  @Override public ResponseReader toBufferedReader() throws IOException {
    Map<String, Object> buffer = toMap(this);
    return new BufferedResponseReader(buffer, customTypeAdapters);
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
          case CUSTOM:
            handler.handle(fieldIndex, readCustomType(field));
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

  String nextString(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextString();
    }
  }

  Integer nextInt(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextInt();
    }
  }

  Long nextLong(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextLong();
    }
  }

  Double nextDouble(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextDouble();
    }
  }

  Boolean nextBoolean(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextBoolean();
    }
  }

  <T> T nextObject(boolean optional, Field.ObjectReader<T> objectReader) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      jsonReader.beginObject();
      T result = objectReader.read(this);
      jsonReader.endObject();
      return result;
    }
  }

  <T> List<T> nextList(boolean optional, Field.ListReader<T> listReader) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      List<T> result = new ArrayList<>();
      jsonReader.beginArray();
      while (jsonReader.hasNext()) {
        T item = listReader.read(new JsonListItemReader(this));
        result.add(item);
      }
      jsonReader.endArray();
      return result;
    }
  }

  <T> List<T> nextList(boolean optional, Field.ObjectReader<T> objectReader) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      List<T> result = new ArrayList<>();
      jsonReader.beginArray();
      while (jsonReader.hasNext()) {
        jsonReader.beginObject();
        T item = objectReader.read(this);
        jsonReader.endObject();
        result.add(item);
      }
      jsonReader.endArray();
      return result;
    }
  }

  @SuppressWarnings("unchecked") <T> T nextCustomType(boolean optional, TypeMapping typeMapping) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      CustomTypeAdapter<T> typeAdapter = customTypeAdapters.get(typeMapping);
      if (typeAdapter == null) {
        throw new RuntimeException("Can't resolve custom type adapter for " + typeMapping.type());
      }
      String value = jsonReader.nextString();
      return typeAdapter.decode(value);
    }
  }

  private String readString(Field field) throws IOException {
    return nextString(field.optional());
  }

  private Integer readInt(Field field) throws IOException {
    return nextInt(field.optional());
  }

  private Long readLong(Field field) throws IOException {
    return nextLong(field.optional());
  }

  private Double readDouble(Field field) throws IOException {
    return nextDouble(field.optional());
  }

  private Boolean readBoolean(Field field) throws IOException {
    return nextBoolean(field.optional());
  }

  @SuppressWarnings("unchecked") private <T> T readObject(Field field) throws IOException {
    return (T) nextObject(field.optional(), field.objectReader());
  }

  @SuppressWarnings("unchecked") private <T> List<T> readList(Field field) throws IOException {
    if (field.objectReader() != null) {
      return nextList(field.optional(), field.objectReader());
    } else {
      return nextList(field.optional(), field.listReader());
    }
  }

  private <T> T readCustomType(Field field) throws IOException {
    return nextCustomType(field.optional(), field.typeMapping());
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

  private void checkNextValue(boolean optional) throws IOException {
    if (!optional && jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("corrupted response reader, expected non null value");
    }
  }

  private static Map<String, Object> toMap(ResponseJsonStreamReader streamReader) throws IOException {
    if (streamReader.isNextObject()) {
      return readObject(streamReader);
    }

    Map<String, Object> result = new LinkedHashMap<>();
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
    return streamReader.nextObject(false, new Field.ObjectReader<Map<String, Object>>() {
      @Override public Map<String, Object> read(ResponseReader streamReader) throws IOException {
        return toMap((ResponseJsonStreamReader) streamReader);
      }
    });
  }

  private static List<?> readList(final ResponseJsonStreamReader streamReader) throws IOException {
    return streamReader.nextList(false, new Field.ListReader<Object>() {
      @Override public Object read(Field.ListItemReader reader) throws IOException {
        if (streamReader.isNextObject()) {
          return readObject(streamReader);
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
      return streamReader.nextBoolean(false);
    } else if (streamReader.isNextNumber()) {
      return new BigDecimal(streamReader.nextString(false));
    } else {
      return streamReader.nextString(false);
    }
  }

  private static class JsonListItemReader implements Field.ListItemReader {

    final ResponseJsonStreamReader streamReader;

    JsonListItemReader(ResponseJsonStreamReader streamReader) {
      this.streamReader = streamReader;
    }

    @Override public String readString() throws IOException {
      return streamReader.nextString(false);
    }

    @Override public Integer readInt() throws IOException {
      return streamReader.nextInt(false);
    }

    @Override public Long readLong() throws IOException {
      return streamReader.nextLong(false);
    }

    @Override public Double readDouble() throws IOException {
      return streamReader.nextDouble(false);
    }

    @Override public Boolean readBoolean() throws IOException {
      return streamReader.nextBoolean(false);
    }

    @Override public <T> T readCustomType(TypeMapping typeMapping) throws IOException {
      return streamReader.nextCustomType(false, typeMapping);
    }
  }
}