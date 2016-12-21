package com.apollostack.android;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

public class ApolloConverterFactory extends Converter.Factory {
  private final Moshi moshi;
  private final MoshiConverterFactory moshiConverterFactory;

  public ApolloConverterFactory(Moshi moshi) {
    this.moshi = moshi;
    moshiConverterFactory = MoshiConverterFactory.create(moshi);
  }

  @Override public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    JsonAdapter<?> adapter = moshi.adapter(type);
    return new ApolloResponseBodyConverter<>(adapter);
  }

  @Override public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations, Retrofit retrofit) {
    return moshiConverterFactory.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit);
  }
}
