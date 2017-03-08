package com.apollographql.android.cache.normalized.sql;

import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import org.junit.Before;

/**
 * Created by 206847 on 3/8/17.
 */

public class SqlStoreTest extends AndroidTestCase {

  public static final String KEY = "key";
  public static final String RECORD = "record";
  private SqlStore sqlStore;

  @Before
  public void setUp() throws Exception {
    RenamingDelegatingContext context
        = new RenamingDelegatingContext(getContext(), "test_");

    sqlStore = SqlStore.create(new ApolloSqlHelper(context));
  }


}
