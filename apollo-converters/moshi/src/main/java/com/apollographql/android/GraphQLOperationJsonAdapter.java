package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;

class GraphQLOperationJsonAdapter extends JsonAdapter<Operation> {
  private final Moshi moshi;

  GraphQLOperationJsonAdapter(Moshi moshi) {
    this.moshi = moshi;
  }

  @Override public Operation fromJson(JsonReader reader) throws IOException {
    throw new IllegalStateException("This should not be called ever.");
  }

  @Override public void toJson(JsonWriter writer, Operation value) throws IOException {
    writer.beginObject();
    writer.name("query").value(value.queryDocument().replaceAll("\\n", ""));
    Operation.Variables variables = value.variables();
    if (variables != null) {
      //noinspection unchecked
      JsonAdapter<Operation.Variables> adapter =
          (JsonAdapter<Operation.Variables>) moshi.adapter(variables.getClass());
      writer.name("variables");
      adapter.toJson(writer, variables);
    }
    writer.endObject();
  }
}
