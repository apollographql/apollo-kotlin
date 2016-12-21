package com.apollostack.android;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Converter;

class ApolloResponseBodyConverter<T> implements Converter<ResponseBody, T> {
  private final JsonAdapter<?> adapter;

  ApolloResponseBodyConverter(JsonAdapter<?> adapter) {
    this.adapter = adapter;
  }

  @Override public T convert(ResponseBody value) throws IOException {
    try {
      JsonReader reader = JsonReader.of(value.source());
      reader.beginObject();
      if (!reader.nextName().equals("data")) {
        throw new IllegalStateException("Malformed input JSON. Expected 'data' object");
      }
      T responseBody = (T) adapter.fromJson(reader);
      reader.endObject();
      return responseBody;
    } finally {
      value.close();
    }
  }
}
