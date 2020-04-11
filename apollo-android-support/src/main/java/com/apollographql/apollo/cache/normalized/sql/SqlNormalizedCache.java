package com.apollographql.apollo.cache.normalized.sql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.CacheKey;
import com.apollographql.apollo.cache.normalized.CacheReference;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import static com.apollographql.apollo.api.internal.Utils.checkNotNull;
import static com.apollographql.apollo.cache.ApolloCacheHeaders.DO_NOT_STORE;
import static com.apollographql.apollo.cache.ApolloCacheHeaders.EVICT_AFTER_READ;
import static com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper.COLUMN_KEY;
import static com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper.COLUMN_RECORD;
import static com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper.TABLE_RECORDS;

public final class SqlNormalizedCache extends NormalizedCache {
  private static final String INSERT_STATEMENT =
      String.format("INSERT INTO %s (%s,%s) VALUES (?,?)",
          TABLE_RECORDS,
          COLUMN_KEY,
          COLUMN_RECORD);
  private static final String UPDATE_STATEMENT =
      String.format("UPDATE %s SET %s=?, %s=? WHERE %s=?",
          TABLE_RECORDS,
          COLUMN_KEY,
          COLUMN_RECORD,
          COLUMN_KEY);
  private static final String DELETE_STATEMENT =
      String.format("DELETE FROM %s WHERE %s=?",
          TABLE_RECORDS,
          COLUMN_KEY);
  private static final String DELETE_ALL_RECORD_STATEMENT = String.format("DELETE FROM %s", TABLE_RECORDS);
  SQLiteDatabase database;
  private final ApolloSqlHelper dbHelper;
  private final String[] allColumns = {ApolloSqlHelper.COLUMN_ID,
      ApolloSqlHelper.COLUMN_KEY,
      ApolloSqlHelper.COLUMN_RECORD};

  private final SQLiteStatement insertStatement;
  private final SQLiteStatement updateStatement;
  private final SQLiteStatement deleteStatement;
  private final SQLiteStatement deleteAllRecordsStatement;
  private final RecordFieldJsonAdapter recordFieldAdapter;

  SqlNormalizedCache(RecordFieldJsonAdapter recordFieldAdapter, ApolloSqlHelper dbHelper) {
    this.recordFieldAdapter = recordFieldAdapter;
    this.dbHelper = dbHelper;
    database = dbHelper.getWritableDatabase();
    insertStatement = database.compileStatement(INSERT_STATEMENT);
    updateStatement = database.compileStatement(UPDATE_STATEMENT);
    deleteStatement = database.compileStatement(DELETE_STATEMENT);
    deleteAllRecordsStatement = database.compileStatement(DELETE_ALL_RECORD_STATEMENT);
  }

  @Override
  @Nullable public Record loadRecord(@NotNull final String key, @NotNull final CacheHeaders cacheHeaders) {
    Record record = selectRecordForKey(key);

    if (record != null) {
      if (cacheHeaders.hasHeader(EVICT_AFTER_READ)) {
        deleteRecord(key);
      }
      return record;
    }

    NormalizedCache nextCache = getNextCache();
    if (nextCache != null) {
      return nextCache.loadRecord(key, cacheHeaders);
    }
    return null;
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @NotNull @Override public Set<String> merge(@NotNull final Collection<Record> recordSet,
      @NotNull final CacheHeaders cacheHeaders) {
    if (cacheHeaders.hasHeader(DO_NOT_STORE)) {
      return Collections.emptySet();
    }

    Set<String> changedKeys;
    try {
      database.beginTransaction();
      changedKeys = super.merge(recordSet, cacheHeaders);
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
    return changedKeys;
  }

  @Override public void clearAll() {
    NormalizedCache nextCache = getNextCache();
    if (nextCache != null) {
      nextCache.clearAll();
    }
    clearCurrentCache();
  }

  @Override public boolean remove(@NotNull final CacheKey cacheKey, final boolean cascade) {
    checkNotNull(cacheKey, "cacheKey == null");

    boolean result = false;
    NormalizedCache nextCache = getNextCache();
    if (nextCache != null) {
      result = nextCache.remove(cacheKey, cascade);
    }

    if (cascade) {
      return result || removeRecord(selectRecordForKey(cacheKey.getKey()));
    } else {
      return result || deleteRecord(cacheKey.getKey());
    }
  }

  private boolean removeRecord(Record record) {
    if (record != null) {
      boolean result = true;
      for (CacheReference cacheReference : record.referencedFields()) {
        result = result && remove(new CacheKey(cacheReference.key()), true);
      }
      return result;
    } else {
      return false;
    }
  }

  public void close() {
    dbHelper.close();
  }

  long createRecord(String key, String fields) {
    insertStatement.bindString(1, key);
    insertStatement.bindString(2, fields);

    long recordId = insertStatement.executeInsert();
    return recordId;
  }

  void updateRecord(String key, String fields) {
    updateStatement.bindString(1, key);
    updateStatement.bindString(2, fields);
    updateStatement.bindString(3, key);

    updateStatement.executeInsert();
  }

  boolean deleteRecord(String key) {
    deleteStatement.bindString(1, key);
    return deleteStatement.executeUpdateDelete() > 0;
  }

  @Nullable Record selectRecordForKey(String key) {
    Cursor cursor = database.query(ApolloSqlHelper.TABLE_RECORDS,
        allColumns, ApolloSqlHelper.COLUMN_KEY + " = ?", new String[]{key},
        null, null, null);
    if (cursor == null) {
      return null;
    }
    try {
      if (!cursor.moveToFirst()) {
        return null;
      }
      return cursorToRecord(cursor);
    } catch (IOException ignored) {
      return null;
    } finally {
      cursor.close();
    }
  }

  Record cursorToRecord(Cursor cursor) throws IOException {
    String key = cursor.getString(1);
    String jsonOfFields = cursor.getString(2);
    return Record.builder(key).addFields(recordFieldAdapter.from(jsonOfFields)).build();
  }

  void clearCurrentCache() {
    deleteAllRecordsStatement.execute();
  }

  @NotNull
  protected Set<String> performMerge(@NotNull final Record apolloRecord, @NotNull final CacheHeaders cacheHeaders) {
    Record optionalOldRecord = selectRecordForKey(apolloRecord.key());
    Set<String> changedKeys;
    if (optionalOldRecord == null) {
      createRecord(apolloRecord.key(), recordFieldAdapter.toJson(apolloRecord.fields()));
      changedKeys = Collections.emptySet();
    } else {
      changedKeys = optionalOldRecord.mergeWith(apolloRecord);
      if (!changedKeys.isEmpty()) {
        updateRecord(optionalOldRecord.key(), recordFieldAdapter.toJson(optionalOldRecord.fields()));
      }
    }
    return changedKeys;
  }
}
