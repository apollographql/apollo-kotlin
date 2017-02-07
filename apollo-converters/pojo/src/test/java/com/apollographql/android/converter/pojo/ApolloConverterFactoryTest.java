package com.apollographql.android.converter.pojo;

import com.apollographql.android.api.graphql.Operation;
import com.apollographql.android.api.graphql.Response;
import com.apollographql.android.api.graphql.ResponseFieldMapper;
import com.apollographql.android.api.graphql.ResponseReader;
import com.apollographql.android.converter.pojo.type.CustomType;
import com.squareup.moshi.Types;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import okhttp3.RequestBody;
import okio.Buffer;
import retrofit2.Converter;

import static com.google.common.truth.Truth.assertThat;

public class ApolloConverterFactoryTest {
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
  private final Annotation[] annotations = new Annotation[0];
  private ApolloConverterFactory converterFactory;

  @Before public void setUp() {
    CustomTypeAdapter<Date> dateCustomTypeAdapter = new CustomTypeAdapter<Date>() {
      @Override public Date decode(String value) {
        try {
          return DATE_FORMAT.parse(value);
        } catch (ParseException e) {
          throw new RuntimeException(e);
        }
      }

      @Override public String encode(Date value) {
        return DATE_FORMAT.format(value);
      }
    };

    converterFactory = new ApolloConverterFactory.Builder()
        .withResponseFieldMapper(TestData.class, new ResponseFieldMapper<TestData>() {
          @Override public TestData map(ResponseReader responseReader) throws IOException {
            return new TestData();
          }
        })
        .withCustomTypeAdapter(CustomType.DATETIME, dateCustomTypeAdapter)
        .build();
  }

  @Test public void requestBodyDoesNotApplyToOtherClasses() {
    assertThat(converterFactory.requestBodyConverter(Foo.class, annotations, annotations, null)).isNull();
  }

  @Test public void requestBodyAppliesToPostBodyClass() {
    Type type = Types.newParameterizedType(OperationRequest.class, TestData.Variables.class);
    assertThat(converterFactory.requestBodyConverter(type, annotations, annotations, null)).isNotNull();
  }

  @Test public void responseDoesNotApplyToOtherClasses() {
    assertThat(converterFactory.responseBodyConverter(Foo.class, annotations, null)).isNull();
  }

  @Test public void responseAppliesToDataClass() {
    Type type = Types.newParameterizedType(Response.class, TestData.class);
    assertThat(converterFactory.responseBodyConverter(type, annotations, null)).isNotNull();
  }

  @Test public void serializeRequestWithCustomType() throws IOException, ParseException {
    final Date now = DATE_FORMAT.parse("2017-02-06T18:45:42Z");
    final OperationRequest<TestData.Variables> operationRequest = new OperationRequest<TestData.Variables>("", new
        TestData.Variables(now));
    Type type = Types.newParameterizedType(OperationRequest.class, TestData.Variables.class);
    Converter<OperationRequest, RequestBody> converter = converterFactory.requestBodyConverter(type, annotations,
        annotations, null);
    RequestBody requestBody = converter.convert(operationRequest);
    Buffer buffer = new Buffer();
    requestBody.writeTo(buffer);

    assertThat(buffer.toString()).isEqualTo("[text={\"query\":\"\",\"variables\":{\"createdAt\":\"2017-02-06T18:45:42Z\"}}]");
  }

  static class TestData implements Operation.Data {
    static class Variables extends Operation.Variables {
      final Date createdAt;

      Variables(Date createdAt) {
        this.createdAt = createdAt;
      }
    }
  }

  static class Foo {
  }
}
