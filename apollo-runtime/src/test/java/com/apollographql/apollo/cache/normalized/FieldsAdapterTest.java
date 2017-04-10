package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.cache.normalized.sql.FieldsAdapter;

import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;

public class FieldsAdapterTest {

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

    recordBuilder.addField("bigDecimal", expectedBigDecimal);
    recordBuilder.addField("string", expectedStringValue);
    recordBuilder.addField("boolean", expectedBooleanValue);
    recordBuilder.addField("cacheReference", expectedCacheReference);
    recordBuilder.addField("scalarList", expectedScalarList);
    recordBuilder.addField("referenceList", expectedCacheReferenceList);
    Record record = recordBuilder.build();

    FieldsAdapter recordAdapter = FieldsAdapter.create();
    String json = recordAdapter.toJson(record.fields());
    Map<String, Object> map = recordAdapter.from(json);
    assertThat(map.get("bigDecimal")).isEqualTo(expectedBigDecimal);
    assertThat(map.get("string")).isEqualTo(expectedStringValue);
    assertThat(map.get("boolean")).isEqualTo(expectedBooleanValue);
    assertThat(map.get("cacheReference")).isEqualTo(expectedCacheReference);
    assertThat((Iterable)map.get("scalarList")).containsExactlyElementsIn(expectedScalarList).inOrder();
    assertThat((Iterable)map.get("referenceList")).containsExactlyElementsIn(expectedCacheReferenceList).inOrder();
  }
}
