package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.response.CustomTypeAdapter;
import com.apollographql.apollo.response.CustomTypeValue;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class RecordFieldAdapterTest {

  RecordFieldJsonAdapter recordFieldAdapter;
  CustomTypeAdapter<TestCustomScalar> customTypeAdapter;

  @Before public void setUpAdapter() {
    customTypeAdapter = new CustomTypeAdapter<TestCustomScalar>() {

      @Override public TestCustomScalar decode(CustomTypeValue value) {
        String valueStr = value.value.toString();
        return new TestCustomScalar(valueStr.substring(1, valueStr.length()));
      }

      @Override public CustomTypeValue encode(TestCustomScalar value) {
        return new CustomTypeValue.GraphQLString("#" + value.fieldOne);
      }
    };
    recordFieldAdapter = RecordFieldJsonAdapter.create();
  }

  @Test
  public void testFieldsAdapterSerializationDeserialization() throws IOException {
    Record.Builder recordBuilder = Record.builder("root");
    BigDecimal expectedBigDecimal = new BigDecimal(1.23);
    String expectedStringValue = "StringValue";
    Boolean expectedBooleanValue = true;
    CacheReference expectedCacheReference = new CacheReference("foo");
    List<CacheReference> expectedCacheReferenceList = Arrays.asList(new CacheReference("bar"), new CacheReference("baz"));
    List<Object> expectedScalarList = Arrays.<Object>asList("scalarOne", "scalarTwo");
    List<List<String>> expectedListOfScalarList = Arrays.asList(Arrays.asList("scalarOne", "scalarTwo"));

    recordBuilder.addField("bigDecimal", expectedBigDecimal);
    recordBuilder.addField("string", expectedStringValue);
    recordBuilder.addField("boolean", expectedBooleanValue);
    recordBuilder.addField("cacheReference", expectedCacheReference);
    recordBuilder.addField("scalarList", expectedScalarList);
    recordBuilder.addField("referenceList", expectedCacheReferenceList);
    recordBuilder.addField("nullValue", null);
    recordBuilder.addField("listOfScalarList", expectedListOfScalarList);
    Record record = recordBuilder.build();

    String json = recordFieldAdapter.toJson(record.fields());
    Map<String, Object> deserializedMap = recordFieldAdapter.from(json);
    assertThat(deserializedMap.get("bigDecimal")).isEqualTo(expectedBigDecimal);
    assertThat(deserializedMap.get("string")).isEqualTo(expectedStringValue);
    assertThat(deserializedMap.get("boolean")).isEqualTo(expectedBooleanValue);
    assertThat(deserializedMap.get("cacheReference")).isEqualTo(expectedCacheReference);
    assertThat((Iterable) deserializedMap.get("scalarList")).containsExactlyElementsIn(expectedScalarList).inOrder();
    assertThat((Iterable) deserializedMap.get("referenceList")).containsExactlyElementsIn(expectedCacheReferenceList).inOrder();
    assertThat(deserializedMap.containsKey("nullValue")).isTrue();
    assertThat(deserializedMap.get("nullValue")).isNull();
    assertThat((List) deserializedMap.get("listOfScalarList")).hasSize(1);
    assertThat((Iterable) ((List) deserializedMap.get("listOfScalarList")).get(0)).containsExactlyElementsIn(expectedScalarList).inOrder();
  }
}
