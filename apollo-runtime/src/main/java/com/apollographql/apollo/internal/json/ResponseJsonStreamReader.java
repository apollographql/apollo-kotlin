package com.apollographql.apollo.internal.json;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResponseJsonStreamReader {
  private final JsonReader jsonReader;

  public ResponseJsonStreamReader(JsonReader jsonReader) {
    this.jsonReader = jsonReader;
  }

  public boolean hasNext() throws IOException {
    return jsonReader.hasNext();
  }

  public String nextName() throws IOException {
    return jsonReader.nextName();
  }

  public void skipNext() throws IOException {
    jsonReader.skipValue();
  }

  public String nextString(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextString();
    }
  }

  public Integer nextInt(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextInt();
    }
  }

  public Long nextLong(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextLong();
    }
  }

  public Double nextDouble(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextDouble();
    }
  }

  public Boolean nextBoolean(boolean optional) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      return jsonReader.nextBoolean();
    }
  }

  public <T> T nextObject(boolean optional, ObjectReader<T> objectReader) throws IOException {
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

  public <T> List<T> nextList(boolean optional, ListReader<T> listReader) throws IOException {
    checkNextValue(optional);
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    } else {
      List<T> result = new ArrayList<>();
      jsonReader.beginArray();
      while (jsonReader.hasNext()) {
        T item = listReader.read(this);
        result.add(item);
      }
      jsonReader.endArray();
      return result;
    }
  }

  public Object nextScalar(boolean optional) throws IOException {
    checkNextValue(optional);
    if (isNextNull()) {
      skipNext();
      return null;
    } else if (isNextBoolean()) {
      return nextBoolean(false);
    } else if (isNextNumber()) {
      return new BigDecimal(nextString(false));
    } else {
      return nextString(false);
    }
  }

  public Map<String, Object> toMap() throws IOException {
    if (isNextObject()) {
      return readObject(this);
    }

    Map<String, Object> result = new LinkedHashMap<>();
    while (hasNext()) {
      String name = nextName();
      if (isNextNull()) {
        skipNext();
        result.put(name, null);
      } else if (isNextObject()) {
        result.put(name, readObject(this));
      } else if (isNextList()) {
        result.put(name, readList(this));
      } else {
        result.put(name, nextScalar(true));
      }
    }
    return result;
  }

  boolean isNextObject() throws IOException {
    return jsonReader.peek() == JsonReader.Token.BEGIN_OBJECT;
  }

  boolean isNextList() throws IOException {
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

  Map<String, Object> readObject(final ResponseJsonStreamReader streamReader) throws IOException {
    return streamReader.nextObject(false, new ObjectReader<Map<String, Object>>() {
      @Override public Map<String, Object> read(ResponseJsonStreamReader streamReader) throws IOException {
        return streamReader.toMap();
      }
    });
  }

  List<?> readList(final ResponseJsonStreamReader streamReader) throws IOException {
    return streamReader.nextList(false, new ListReader<Object>() {
      @Override public Object read(ResponseJsonStreamReader reader) throws IOException {
        if (streamReader.isNextList()) {
          return readList(reader);
        } else if (streamReader.isNextObject()) {
          return readObject(reader);
        } else {
          return reader.nextScalar(true);
        }
      }
    });
  }

  public interface ObjectReader<T> {
    T read(ResponseJsonStreamReader reader) throws IOException;
  }

  public interface ListReader<T> {
    T read(ResponseJsonStreamReader reader) throws IOException;
  }
}
