package com.apollostack.converter.pojo;

import com.apollostack.api.graphql.Operation;
import com.apollostack.api.graphql.Response;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class ApolloConverterFactory extends Converter.Factory {
  private final Moshi moshi;

  public ApolloConverterFactory(Moshi moshi) {
    this.moshi = moshi;
  }

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

  @Override public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations, Retrofit retrofit) {
    if (Operation.class.isAssignableFrom(Types.getRawType(type))) {
      JsonAdapter<Operation> adapter = new GraphQLOperationJsonAdapter(moshi);
      return new ApolloRequestBodyConverter(adapter);
    } else {
      return null;
    }
  }
}
