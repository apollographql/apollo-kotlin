package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ScalarType;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/** TODO */
public final class ApolloConverterFactory extends Converter.Factory {
  private final Map<Type, ResponseFieldMapper> responseFieldMappers;
  private final Map<ScalarType, CustomTypeAdapter> customTypeAdapters;

  public ApolloConverterFactory(Map<Type, ResponseFieldMapper> responseFieldMappers,
      Map<ScalarType, CustomTypeAdapter> customTypeAdapters) {
    this.responseFieldMappers = responseFieldMappers;
    this.customTypeAdapters = customTypeAdapters;
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

    public Builder withCustomTypeAdapter(@Nonnull ScalarType scalarType,
        @Nonnull CustomTypeAdapter customTypeAdapter) {
      customTypeAdapters.put(scalarType, customTypeAdapter);
      return this;
    }

    public Builder withResponseFieldMapper(@Nonnull Type type, @Nonnull ResponseFieldMapper responseFieldMapper) {
      responseFieldMappers.put(type, responseFieldMapper);
      return this;
    }

    public ApolloConverterFactory build() {
      return new ApolloConverterFactory(responseFieldMappers, customTypeAdapters);
    }
  }
}
