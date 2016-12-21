package com.apollostack.android;

import com.apollostack.api.GraphQLOperation;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

class GraphQLOperationJsonAdapter<T extends GraphQLOperation.Variables> extends JsonAdapter<GraphQLOperation> {
  private final Moshi moshi;

  GraphQLOperationJsonAdapter(Moshi moshi) {
    this.moshi = moshi;
  }

  @Override public GraphQLOperation fromJson(JsonReader reader) throws IOException {
    throw new IllegalStateException("This should not be called ever.");
  }

  @Override public void toJson(JsonWriter writer, GraphQLOperation value) throws IOException {
    writer.beginObject();
    writer.name("query").value(value.queryDocument().replaceAll("\\n", ""));
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
