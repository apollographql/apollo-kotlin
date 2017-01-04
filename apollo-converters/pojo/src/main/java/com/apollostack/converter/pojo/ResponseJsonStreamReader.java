package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.ResponseReader;
import com.apollostack.api.graphql.ResponseStreamReader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

  @Override public String nextString() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextString();
  }

  @Override public String nextOptionalString() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextString();
  }

  @Override public int nextInt() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextInt();
  }

  @Override public Integer nextOptionalInt() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextInt();
  }

  @Override public long nextLong() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextLong();
  }

  @Override public Long nextOptionalLong() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextLong();
  }

  @Override public double nextDouble() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextDouble();
  }

  @Override public Double nextOptionalDouble() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextDouble();
  }

  @Override public boolean nextBoolean() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return jsonReader.nextBoolean();
  }

  @Override public Boolean nextOptionalBoolean() throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }
    return jsonReader.nextBoolean();
  }

  @Override public <T> T nextObject(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return nextOptionalObject(nestedReader);
  }

  @Override public <T> T nextOptionalObject(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      jsonReader.skipValue();
      return null;
    }

    jsonReader.beginObject();
    T result = nestedReader.read(this);
    jsonReader.endObject();
    return result;
  }

  @Override public <T> List<T> nextList(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
      throw new NullPointerException("can't parse response, expected non null json value");
    }
    return nexOptionalList(nestedReader);
  }

  @Override public <T> List<T> nexOptionalList(NestedReader<T> nestedReader) throws IOException {
    if (jsonReader.peek() == JsonReader.Token.NULL) {
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

  @Override public ResponseReader toBufferedReader() throws IOException {
    return BufferedResponseReader.fromStreamReader(this);
  }
}
