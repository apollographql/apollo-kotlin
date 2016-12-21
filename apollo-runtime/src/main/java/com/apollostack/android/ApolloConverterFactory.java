package com.apollostack.android;

import com.apollostack.api.GraphQLQuery;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import static com.squareup.moshi.Types.getRawType;

public class ApolloConverterFactory extends Converter.Factory {
  private final Moshi moshi;

  public ApolloConverterFactory(Moshi moshi) {
    this.moshi = moshi;
  }

  @Override public Converter<ResponseBody, ?> responseBodyConverter(Type type, Annotation[] annotations,
      Retrofit retrofit) {
    if (isApplicableForType(type)) {
      JsonAdapter<?> adapter = moshi.adapter(type);
      return new ApolloResponseBodyConverter<>(adapter);
    } else {
      return null;
    }
  }

  @Override public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations, Retrofit retrofit) {
    // This converter is only for parsing ResponseBody
    return null;
  }

  private static boolean isApplicableForType(Type type) {
    return GraphQLQuery.Data.class.isAssignableFrom(getRawType(type));
  }
}
