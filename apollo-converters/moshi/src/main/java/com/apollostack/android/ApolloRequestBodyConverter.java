package com.apollostack.android;

import com.apollostack.api.GraphQLOperation;
import com.squareup.moshi.JsonAdapter;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

class ApolloRequestBodyConverter implements Converter<GraphQLOperation, RequestBody> {
  private static final MediaType MEDIA_TYPE = MediaType.parse("application/json; charset=UTF-8");
  private final JsonAdapter<GraphQLOperation> adapter;

  ApolloRequestBodyConverter(JsonAdapter<GraphQLOperation> adapter) {
    this.adapter = adapter;
  }

  @Override public RequestBody convert(GraphQLOperation value) throws IOException {
    Buffer buffer = new Buffer();
    adapter.toJson(buffer, value);
    return RequestBody.create(MEDIA_TYPE, buffer.readByteString());
  }
}
