package com.apollostack.android;

import com.apollostack.api.GraphQLOperation;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

class PostBodyJsonAdapter<T extends GraphQLOperation.Variables> extends JsonAdapter<PostBody> {
  private final Moshi moshi;

  PostBodyJsonAdapter(Moshi moshi) {
    this.moshi = moshi;
  }

  @Override public PostBody fromJson(JsonReader reader) throws IOException {
    throw new IllegalStateException("This should not be called ever.");
  }

  @Override public void toJson(JsonWriter writer, PostBody value) throws IOException {
    writer.beginObject();
    writer.name("query").value(value.query());
    //noinspection unchecked
    T variables = (T) value.variables();
    if (variables != null) {
      //noinspection unchecked
      JsonAdapter<T> adapter = (JsonAdapter<T>) moshi.adapter(variables.getClass());
      writer.name("variables");
      adapter.toJson(writer, variables);
    }
    writer.endObject();
  }
}
