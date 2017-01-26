package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.TypeMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

/** TODO */
public class ApolloConverterFactory extends Converter.Factory {
  private final Map<TypeMapping, CustomTypeAdapter> customTypeAdapters = new HashMap<>();

  @Override public Converter<ResponseBody, Response<? extends Operation.Data>> responseBodyConverter(Type type,
      Annotation[] annotations, Retrofit retrofit) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      if (Response.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
        return new ApolloResponseBodyConverter(parameterizedType.getActualTypeArguments()[0], customTypeAdapters);
      }
    }
    return null;
  }

  public ApolloConverterFactory withCustomTypeAdapter(@Nonnull TypeMapping typeMapping,
      @Nonnull CustomTypeAdapter customTypeAdapter) {
    customTypeAdapters.put(typeMapping, customTypeAdapter);
    return this;
  }
}
