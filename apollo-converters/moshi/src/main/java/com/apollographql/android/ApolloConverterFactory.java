package com.apollographql.android;

import com.apollographql.api.graphql.Operation;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

import static com.squareup.moshi.Types.getRawType;

@SuppressWarnings("WeakerAccess")
public class ApolloConverterFactory extends Converter.Factory {
  private final Moshi moshi;

  public ApolloConverterFactory(Moshi moshi) {
    this.moshi = moshi;
  }

  @Override public Converter<?, RequestBody> requestBodyConverter(Type type, Annotation[] parameterAnnotations,
      Annotation[] methodAnnotations, Retrofit retrofit) {
    if (Operation.class.isAssignableFrom(getRawType(type))) {
      JsonAdapter<Operation> adapter = new GraphQLOperationJsonAdapter(moshi);
      return new ApolloRequestBodyConverter(adapter);
    } else {
      return null;
    }
  }
}
