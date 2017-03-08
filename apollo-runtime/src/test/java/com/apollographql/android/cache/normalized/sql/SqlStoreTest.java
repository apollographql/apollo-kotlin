package com.apollographql.android.cache.normalized.sql;

import android.database.sqlite.SQLiteDatabase;
import android.provider.Contacts;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import com.apollographql.android.cache.normalized.Record;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import static android.provider.Contacts.SettingsColumns.KEY;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

/**
 * Created by 206847 on 3/8/17.
 */

public class SqlStoreTest extends AndroidTestCase {

  public static final String KEY = "key";
  public static final String RECORD="record";
  private SqlStore sqlStore;

  @Before
  public void setUp() throws Exception {
    RenamingDelegatingContext context
        = new RenamingDelegatingContext(getContext(), "test_");

    sqlStore = SqlStore.create(new ApolloSqlHelper(context));
  }


}
