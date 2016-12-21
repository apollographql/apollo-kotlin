package com.apollostack.android;

import com.apollostack.api.GraphQLOperation;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

class GraphQLOperationJsonAdapter extends JsonAdapter<GraphQLOperation> {
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
    GraphQLOperation.Variables variables = value.variables();
    if (variables != null) {
      //noinspection unchecked
      JsonAdapter<GraphQLOperation.Variables> adapter =
          (JsonAdapter<GraphQLOperation.Variables>) moshi.adapter(variables.getClass());
      writer.name("variables");
      adapter.toJson(writer, variables);
    }
    writer.endObject();
  }
}
