package com.apollographql.android.cache.normalized.sql;

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.apollographql.android.api.graphql.internal.Optional;
import com.apollographql.android.cache.normalized.FieldsAdapter;
import com.apollographql.android.cache.normalized.Record;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.truth.Truth.assertThat;

@RunWith(AndroidJUnit4.class)
public class SqlStoreTest {

  public static final String KEY = "key";
  public static final String FIELDS = "{\"fieldKey\": \"value\"}";
  public static final String IN_MEMORY_DB = null; //null means db is memory only
  private SqlStore sqlStore;

  @Before
  public void setUp() {
    ApolloSqlHelper apolloSqlHelper = ApolloSqlHelper.create(InstrumentationRegistry.getTargetContext(),
        IN_MEMORY_DB);
    sqlStore = SqlStore.create(apolloSqlHelper, FieldsAdapter.create());
  }

  @Test
  public void testRecordCreation() {
    long record = createRecord();
    assertThat(record).isEqualTo(1);
  }

  @Test
  public void testRecordDeletion() {
    long recordId = createRecord();
    sqlStore.deleteRecord(KEY);
  }

  @Test
  public void testRecordSelection() {
    createRecord();
    Optional<Record> record = sqlStore.selectRecordForKey(KEY);
    assertThat(record.get().key()).isEqualTo(KEY);
  }

  private long createRecord() {
    return sqlStore.createRecord(KEY, FIELDS);
  }
}
