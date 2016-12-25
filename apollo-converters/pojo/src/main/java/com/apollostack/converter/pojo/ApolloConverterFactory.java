package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Response;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class ApolloConverterFactory extends Converter.Factory {
  @Override
  public Converter<ResponseBody, Response<? extends Operation.Data>> responseBodyConverter(Type type,
      Annotation[] annotations, Retrofit retrofit) {
    if (Operation.Data.class.isAssignableFrom((Class<?>) type)) {
      return new ApolloResponseBodyConverter(type);
    }
    return null;
  }
}
