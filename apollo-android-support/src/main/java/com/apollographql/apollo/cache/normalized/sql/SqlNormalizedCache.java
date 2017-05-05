package com.apollographql.apollo.cache.normalized.sql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.apollographql.apollo.api.internal.Optional;
import com.apollographql.apollo.cache.CacheHeaders;
import com.apollographql.apollo.cache.normalized.NormalizedCache;
import com.apollographql.apollo.cache.normalized.Record;
import com.apollographql.apollo.cache.normalized.RecordFieldAdapter;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.cache.ApolloCacheHeaders.EVICT_AFTER_READ;
import static com.apollographql.apollo.cache.ApolloCacheHeaders.DO_NOT_STORE;
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

  SqlNormalizedCache(RecordFieldAdapter recordFieldAdapter, ApolloSqlHelper dbHelper) {
    super(recordFieldAdapter);
    this.dbHelper = dbHelper;
    database = dbHelper.getWritableDatabase();
    insertStatement = database.compileStatement(INSERT_STATEMENT);
    updateStatement = database.compileStatement(UPDATE_STATEMENT);
    deleteStatement = database.compileStatement(DELETE_STATEMENT);
    deleteAllRecordsStatement = database.compileStatement(DELETE_ALL_RECORD_STATEMENT);
  }

  @Nullable public Record loadRecord(@Nonnull String key, @Nonnull CacheHeaders cacheHeaders) {
    Record record = selectRecordForKey(key).orNull();
    if (cacheHeaders.hasHeader(EVICT_AFTER_READ) && record != null) {
      deleteRecord(key);
    }
    return record;
  }

  @Nonnull public Set<String> merge(@Nonnull Record apolloRecord, @Nonnull CacheHeaders cacheHeaders) {
    if (cacheHeaders.hasHeader(DO_NOT_STORE)) {
      return Collections.emptySet();
    }
    Optional<Record> optionalOldRecord = selectRecordForKey(apolloRecord.key());
    Set<String> changedKeys;
    if (!optionalOldRecord.isPresent()) {
      createRecord(apolloRecord.key(), recordAdapter().toJson(apolloRecord.fields()));
      changedKeys = Collections.emptySet();
    } else {
      Record oldRecord = optionalOldRecord.get();
      changedKeys = oldRecord.mergeWith(apolloRecord);
      if (!changedKeys.isEmpty()) {
        updateRecord(oldRecord.key(), recordAdapter().toJson(oldRecord.fields()));
      }
    }
    return changedKeys;
  }

  @Nonnull @Override public Set<String> merge(@Nonnull Collection<Record> recordSet,
      @Nonnull CacheHeaders cacheHeaders) {
    if (cacheHeaders.hasHeader(DO_NOT_STORE)) {
      return Collections.emptySet();
    }
    Set<String> changedKeys = Collections.emptySet();
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
    deleteAllRecordsStatement.execute();
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

  void deleteRecord(String key) {
    deleteStatement.bindString(1, key);
    deleteStatement.executeUpdateDelete();
  }

  Optional<Record> selectRecordForKey(String key) {
    Cursor cursor = database.query(ApolloSqlHelper.TABLE_RECORDS,
        allColumns, ApolloSqlHelper.COLUMN_KEY + " = ?", new String[]{key},
        null, null, null);
    if (cursor == null || !cursor.moveToFirst()) {
      return Optional.absent();
    }
    try {
      return Optional.of(cursorToRecord(cursor));
    } catch (IOException exception) {
      return Optional.absent();
    } finally {
      cursor.close();
    }
  }

  Record cursorToRecord(Cursor cursor) throws IOException {
    String key = cursor.getString(1);
    String jsonOfFields = cursor.getString(2);
    return new Record(key, recordAdapter().from(jsonOfFields));
  }

  public void close() {
    dbHelper.close();
  }
}
