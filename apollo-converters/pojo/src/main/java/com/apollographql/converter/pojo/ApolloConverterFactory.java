package com.apollographql.converter.pojo;

import com.apollographql.api.graphql.Operation;
import com.apollographql.api.graphql.Response;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class ApolloConverterFactory extends Converter.Factory {
  @Override
  public Converter<ResponseBody, Response<? extends Operation.Data>> responseBodyConverter(Type type,
      Annotation[] annotations, Retrofit retrofit) {
    if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;
      if (Response.class.isAssignableFrom((Class<?>) parameterizedType.getRawType())) {
        return new ApolloResponseBodyConverter(parameterizedType.getActualTypeArguments()[0]);
      }
    }
    return null;
  }
}
