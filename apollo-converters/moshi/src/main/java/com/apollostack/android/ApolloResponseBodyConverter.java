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
      /* TODO handle other possible situations as described in the spec: http://facebook.github.io/graphql/#sec-Data
       * If an error was encountered before execution begins, the data entry should not be present in the result.
       * If an error was encountered during the execution that prevented a valid response, the data entry in the
       * response should be null.
       */
      // TODO add support for fragments
      if (!reader.nextName().equals("data")) {
        throw new IllegalStateException("Malformed input JSON. Expected 'data' object");
      }
      T data = (T) adapter.fromJson(reader);
      reader.endObject();
      return data;
    } finally {
      value.close();
    }
  }
}
