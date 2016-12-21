package com.apollostack.android;

import com.apollostack.api.GraphQLOperation;
import com.squareup.moshi.Moshi;

import org.junit.Test;

import java.lang.annotation.Annotation;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;

import static com.google.common.truth.Truth.assertThat;

public class ApolloConverterFactoryTest {
  private final Annotation[] annotations = new Annotation[0];
  private final Moshi moshi = new Moshi.Builder().build();
  private final ApolloConverterFactory factory = new ApolloConverterFactory(moshi);

  private static class TestData implements GraphQLOperation.Data {
  }

  private static class Foo {
  }

  @Test public void responsebodyAppliesToDataClasses() {
    Converter<ResponseBody, ?> converter = factory.responseBodyConverter(TestData.class, null, null);
    assertThat(converter).isNotNull();
  }

  @Test public void responseBodydoesNotApplyToOtherClasses() {
    Converter<ResponseBody, ?> converter = factory.responseBodyConverter(Foo.class, null, null);
    assertThat(converter).isNull();
  }

  @Test public void requestBodyDoesNotApplyToDataClasses() {
    Converter<?, RequestBody> converter = factory.requestBodyConverter(TestData.class, annotations, annotations, null);
    assertThat(converter).isNull();
  }

  @Test public void requestBodydoesNotApplyToOtherClasses() {
    Converter<?, RequestBody> converter = factory.requestBodyConverter(Foo.class, annotations, annotations, null);
    assertThat(converter).isNull();
  }
}