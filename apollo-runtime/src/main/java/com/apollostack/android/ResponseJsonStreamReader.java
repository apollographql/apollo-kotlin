package com.apollostack.android;

import android.util.JsonReader;
import android.util.JsonToken;

import com.apollostack.api.graphql.BufferedResponseReader;
import com.apollostack.api.graphql.ResponseStreamReader;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResponseJsonStreamReader implements ResponseStreamReader {
  private final JsonReader jsonReader;

  public ResponseJsonStreamReader(JsonReader jsonReader) {
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
    return jsonReader.peek() == JsonToken.BEGIN_OBJECT;
  }

  @Override public boolean isNextList() throws IOException {
    return jsonReader.peek() == JsonToken.BEGIN_ARRAY;
  }

  @Override public boolean isNextNull() throws IOException {
    return jsonReader.peek() == JsonToken.NULL;
  }

  @Override public boolean isNextBoolean() throws IOException {
    return jsonReader.peek() == JsonToken.BOOLEAN;
  }

  @Override public boolean isNextNumber() throws IOException {
    return jsonReader.peek() == JsonToken.NUMBER;
  }

  @Override public String nextString() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextString();
  }

  @Override public String nextOptionalString() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextString();
  }

  @Override public int nextInt() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextInt();
  }

  @Override public Integer nextOptionalInt() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextInt();
  }

  @Override public long nextLong() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextLong();
  }

  @Override public Long nextOptionalLong() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextLong();
  }

  @Override public double nextDouble() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextDouble();
  }

  @Override public Double nextOptionalDouble() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextDouble();
  }

  @Override public boolean nextBoolean() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextBoolean();
  }

  @Override public Boolean nextOptionalBoolean() throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextBoolean();
  }

  @Override public <T> T nextObject(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return nextOptionalObject(nestedReader);
  }

  @Override public <T> T nextOptionalObject(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.skipValue();
      return null;
    }

    jsonReader.beginObject();
    T result = nestedReader.read(this);
    jsonReader.endObject();
    return result;
  }

  @Override public <T> List<T> nextList(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return nexOptionalList(nestedReader);
  }

  @Override public <T> List<T> nexOptionalList(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonToken.NULL) {
      jsonReader.skipValue();
      return null;
    }

    List<T> result = new ArrayList<>();
    jsonReader.beginArray();
    while (jsonReader.hasNext()) {
      result.add(nestedReader.read(this));
    }
    jsonReader.endArray();
    return result;
  }

  @Override public BufferedResponseReader toBufferedReader() throws IOException {
    return new MemoryBufferedResponseReader(toMap());
  }

  private Map<String, Object> toMap() throws IOException {
    Map<String, Object> result = new HashMap<>();
    while (hasNext()) {
      String name = nextName();
      if (isNextNull()) {
        skipNext();
      } else if (isNextObject()) {
        result.put(name, readObject());
      } else if (isNextList()) {
        result.put(name, readList());
      } else {
        result.put(name, readScalar());
      }
    }
    return result;
  }

  private Map<String, Object> readObject() throws IOException {
    return nextObject(new ResponseStreamReader.NestedReader<Map<String, Object>>() {
      @Override public Map<String, Object> read(ResponseStreamReader reader) throws IOException {
        return toMap();
      }
    });
  }

  private List<?> readList() throws IOException {
    return nextList(new ResponseStreamReader.NestedReader() {
      @Override public Object read(ResponseStreamReader reader) throws IOException {
        if (reader.isNextObject()) {
          return readObject();
        } else if (reader.isNextList()) {
          return readList();
        } else {
          return readScalar();
        }
      }
    });
  }

  private Object readScalar() throws IOException {
    if (isNextNull()) {
      skipNext();
      return null;
    } else if (isNextBoolean()) {
      return nextBoolean();
    } else if (isNextNumber()) {
      return new BigDecimal(nextString());
    } else {
      return nextString();
    }
  }
}
