package com.apollographql.android.converter;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonReader;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/** TODO */
public final class ApolloConverterFactory extends Converter.Factory {
  private final Map<Type, ResponseFieldMapper> responseFieldMappers;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;
  private final Moshi moshi;

  ApolloConverterFactory(Map<Type, ResponseFieldMapper> responseFieldMappers,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters, Moshi moshi) {
    this.responseFieldMappers = responseFieldMappers;
    this.customTypeAdapters = customTypeAdapters;
    this.moshi = moshi;
  }

  @Override public Converter<Operation, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations, Retrofit retrofit) {
    if (Operation.class.isAssignableFrom(Types.getRawType(type))) {
      JsonAdapter<Operation> adapter = new OperationJsonAdapter(moshi);
      return new ApolloRequestBodyConverter(adapter);
    } else {
      return null;
    }
  }

  @Override public Converter<ResponseBody, Response<? extends Operation.Data>> responseBodyConverter(Type type,
      Annotation[] annotations, Retrofit retrofit) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      if (Response.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
        ResponseFieldMapper responseMapper = responseFieldMappers.get(parameterizedType.getActualTypeArguments()[0]);
        if (responseMapper == null) {
          throw new RuntimeException("failed to resolve response field mapper for: " + type + ". Did you forget to "
              + "register one");
        }
        return new ApolloResponseBodyConverter(responseMapper, customTypeAdapters);
      }
    }
    return null;
  }

  public static class Builder {
    private final Map<Type, ResponseFieldMapper> responseFieldMappers = new LinkedHashMap<>();
    private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters = new LinkedHashMap<>();
    private final Moshi.Builder moshiBuilder = new Moshi.Builder();

    public <T> Builder withCustomTypeAdapter(@Nonnull ScalarType scalarType,
        @Nonnull final CustomTypeAdapter<T> customTypeAdapter) {
      customTypeAdapters.put(scalarType, customTypeAdapter);
      moshiBuilder.add(scalarType.javaType(), new JsonAdapter<T>() {
        @Override public T fromJson(JsonReader reader) throws IOException {
          return customTypeAdapter.decode(reader.nextString());
        }

        @Override public void toJson(JsonWriter writer, T value) throws IOException {
          writer.value(customTypeAdapter.encode(value));
        }
      });
      return this;
    }

    public <T> Builder withResponseFieldMapper(@Nonnull Class<T> type,
        @Nonnull ResponseFieldMapper<T> responseFieldMapper) {
      responseFieldMappers.put(type, responseFieldMapper);
      return this;
    }

    public ApolloConverterFactory build() {
      return new ApolloConverterFactory(responseFieldMappers, customTypeAdapters, moshiBuilder.build());
    }
  }
}
