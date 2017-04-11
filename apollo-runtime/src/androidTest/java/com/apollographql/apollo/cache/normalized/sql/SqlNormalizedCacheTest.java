package com.apollographql.apollo.cache.normalized.sql;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordFieldAdapter;
import com.squareup.moshi.Moshi;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class SqlNormalizedCacheTest {

  public static final String STANDARD_KEY = "key";
  public static final String QUERY_ROOT_KEY = "QUERY_ROOT";
  public static final String FIELDS = "{\"fieldKey\": \"value\"}";
  public static final String IN_MEMORY_DB = null; //null means db is memory only
  private SqlNormalizedCache sqlStore;

  @Before
  public void setUp() {
    ApolloSqlHelper apolloSqlHelper = ApolloSqlHelper.create(InstrumentationRegistry.getTargetContext(),
        IN_MEMORY_DB);
    sqlStore = new SqlNormalizedCacheFactory(apolloSqlHelper).createNormalizedCache(RecordFieldAdapter.create(new
        Moshi.Builder().build()));
  }

  @Test
  public void testRecordCreation() {
    long record = createRecord(STANDARD_KEY);
    assertThat(record).isEqualTo(1);
  }

  @Test
  public void testRecordCreation_root() {
    long record = createRecord(QUERY_ROOT_KEY);
    assertThat(record).isEqualTo(1);
  }

  @Test
  public void testRecordSelection() {
    createRecord(STANDARD_KEY);
    Optional<Record> record = sqlStore.selectRecordForKey(STANDARD_KEY);
    assertThat(record.get().key()).isEqualTo(STANDARD_KEY);
  }

  @Test
  public void testRecordSelection_root() {
    createRecord(QUERY_ROOT_KEY);
    Optional<Record> record = sqlStore.selectRecordForKey(QUERY_ROOT_KEY);
    assertThat(record.get().key()).isEqualTo(QUERY_ROOT_KEY);
  }

  @Test
  public void testRecordSelection_recordNotPresent() {
    Record record = sqlStore.loadRecord(STANDARD_KEY);
    assertThat(record).isNull();
  }

  @Test
  public void testRecordMerge() {
    createRecord(STANDARD_KEY);
    sqlStore.merge(Record.builder(STANDARD_KEY)
        .addField("fieldKey", "valueUpdated")
        .addField("newFieldKey", true).build());
    Optional<Record> record = sqlStore.selectRecordForKey(STANDARD_KEY);
    assertThat(record.get().fields().get("fieldKey")).isEqualTo("valueUpdated");
    assertThat(record.get().fields().get("newFieldKey")).isEqualTo(true);
  }

  @Test
  public void testClearAll() {
    createRecord(QUERY_ROOT_KEY);
    createRecord(STANDARD_KEY);
    sqlStore.clearAll();
    assertThat(sqlStore.selectRecordForKey(QUERY_ROOT_KEY).isPresent()).isFalse();
    assertThat(sqlStore.selectRecordForKey(STANDARD_KEY).isPresent()).isFalse();
  }

  private long createRecord(String key) {
    return sqlStore.createRecord(key, FIELDS);
  }
}
