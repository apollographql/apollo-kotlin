package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.CustomTypeAdapter;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonWriter;
import com.squareup.moshi.Moshi;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class RecordFieldAdapterTest {

  RecordFieldAdapter recordFieldAdapter;
  CustomTypeAdapter<TestCustomScalar> customTypeAdapter;

  @Before public void setUpAdapter() {
    customTypeAdapter = new CustomTypeAdapter<TestCustomScalar>() {

      @Override public TestCustomScalar decode(String value) {
        return new TestCustomScalar(value.substring(1, value.length()));
      }

      @Override public String encode(TestCustomScalar value) {
        return "#" + value.fieldOne;
      }
    };

    Moshi moshi = new Moshi.Builder().add(TestCustomScalar.class, new JsonAdapter<TestCustomScalar>() {
      @Override
      public TestCustomScalar fromJson(com.squareup.moshi.JsonReader reader) throws IOException {
        return customTypeAdapter.decode(reader.nextString());
      }

      @Override
      public void toJson(JsonWriter writer, TestCustomScalar value) throws IOException {
        //noinspection unchecked
        writer.value(customTypeAdapter.encode(value));
      }
    }).build();

    recordFieldAdapter = RecordFieldAdapter.create(moshi);
  }

  @Test
  public void testFieldsAdapterSerializationDeserialization() throws IOException {
    Record.Builder recordBuilder = Record.builder("root");
    BigDecimal expectedBigDecimal = new BigDecimal(1.23);
    String expectedStringValue = "StringValue";
    Boolean expectedBooleanValue = true;
    CacheReference expectedCacheReference = new CacheReference("foo");
    List<CacheReference> expectedCacheReferenceList = Arrays.asList(new CacheReference("bar"), new CacheReference
        ("baz"));
    List<Object> expectedScalarList = Arrays.<Object>asList("scalarOne", "scalarTwo");
    TestCustomScalar customScalar = new TestCustomScalar("fieldOne");

    recordBuilder.addField("bigDecimal", expectedBigDecimal);
    recordBuilder.addField("string", expectedStringValue);
    recordBuilder.addField("boolean", expectedBooleanValue);
    recordBuilder.addField("cacheReference", expectedCacheReference);
    recordBuilder.addField("customScalar", customScalar);
    recordBuilder.addField("scalarList", expectedScalarList);
    recordBuilder.addField("referenceList", expectedCacheReferenceList);
    Record record = recordBuilder.build();


    String json = recordFieldAdapter.toJson(record.fields());
    Map<String, Object> deserializedMap = recordFieldAdapter.from(json);
    assertThat(deserializedMap.get("bigDecimal")).isEqualTo(expectedBigDecimal);
    assertThat(deserializedMap.get("string")).isEqualTo(expectedStringValue);
    assertThat(deserializedMap.get("boolean")).isEqualTo(expectedBooleanValue);
    assertThat(deserializedMap.get("cacheReference")).isEqualTo(expectedCacheReference);
    assertThat((Iterable) deserializedMap.get("scalarList"))
        .containsExactlyElementsIn(expectedScalarList).inOrder();
    assertThat((Iterable) deserializedMap.get("referenceList"))
        .containsExactlyElementsIn(expectedCacheReferenceList).inOrder();

    //We expect that custom scalars are read back still in their serialized format.
    assertThat(deserializedMap.get("customScalar")).isEqualTo(customTypeAdapter.encode(customScalar));
  }
}
