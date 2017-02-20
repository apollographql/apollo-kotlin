package com.apollographql.android;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.Request;

final class RealApolloCallFactory {
  private static final String ACCEPT_TYPE = "application/json";
  private static final String CONTENT_TYPE = "application/graphql";

  private final HttpUrl baseUrl;
  private final okhttp3.Call.Factory callFactory;
  private final Map<Type, ResponseFieldMapper> responseFieldMappers;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final Moshi moshi;

  RealApolloCallFactory(HttpUrl baseUrl, Call.Factory callFactory,
      Map<Type, ResponseFieldMapper> responseFieldMappers, Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.baseUrl = baseUrl;
    this.callFactory = callFactory;
    this.responseFieldMappers = responseFieldMappers;
    this.customTypeAdapters = customTypeAdapters;
    this.moshi = moshi(customTypeAdapters);
  }

  <T extends Operation> RealApolloCall createRequest(T operation) {
    ResponseFieldMapper responseFieldMapper = responseFieldMappers.get(operation.getClass());
    if (responseFieldMapper == null) {
      throw new RuntimeException("failed to resolve response field mapper for: " + operation.getClass());
    }
    return new RealApolloCall(operation, moshi, callFactory, baseRequest(), responseFieldMapper, customTypeAdapters);
  }

  private Moshi moshi(Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    Moshi.Builder builder = new Moshi.Builder();
    for (final Map.Entry<ScalarType, CustomTypeAdapter> typeAdapterEntry : customTypeAdapters.entrySet()) {
      builder.add(typeAdapterEntry.getKey().javaType(), new JsonAdapter() {
        @Override public Object fromJson(com.squareup.moshi.JsonReader reader) throws IOException {
          return typeAdapterEntry.getValue().decode(reader.nextString());
        }

        @Override public void toJson(JsonWriter writer, Object value) throws IOException {
          //noinspection unchecked
          writer.value(typeAdapterEntry.getValue().encode(value));
        }
      });
    }
    return builder.build();
  }

  private Request baseRequest() {
    return new Request.Builder()
        .url(baseUrl)
        .header("Accept", ACCEPT_TYPE)
        .header("Content-Type", CONTENT_TYPE)
        .build();
  }
}
