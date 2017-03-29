package com.apollographql.apollo.cache.normalized;

import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.Record;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;


import static com.google.common.truth.Truth.assertThat;

public class RecordWeigherTest {

  @Test
  public void testRecordWeigher() {
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
    record.sizeEstimateBytes();

    //It's difficult to say what the "right" size estimate is, so just checking it is has been calculate at all.
    assertThat(record.sizeEstimateBytes()).isNotEqualTo(-1);
  }
}
