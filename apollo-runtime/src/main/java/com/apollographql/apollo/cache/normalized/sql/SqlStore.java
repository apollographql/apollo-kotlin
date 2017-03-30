package com.apollographql.apollo.cache.normalized.sql;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.apollographql.android.api.graphql.internal.Optional;
import com.apollographql.apollo.cache.normalized.CacheStore;
import com.apollographql.apollo.cache.normalized.Record;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper.COLUMN_KEY;
import static com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper.COLUMN_RECORD;
import static com.apollographql.apollo.cache.normalized.sql.ApolloSqlHelper.TABLE_RECORDS;

public final class SqlStore extends CacheStore {
  SQLiteDatabase database;
  private final FieldsAdapter parser;
  private final ApolloSqlHelper dbHelper;
  private final String[] allColumns = {ApolloSqlHelper.COLUMN_ID,
      ApolloSqlHelper.COLUMN_KEY,
      ApolloSqlHelper.COLUMN_RECORD};

  public static SqlStore create(ApolloSqlHelper helper, FieldsAdapter adapter) {
    return new SqlStore(helper, adapter);
  }

  private SqlStore(ApolloSqlHelper dbHelper, FieldsAdapter parser) {
    this.dbHelper = dbHelper;
    database = dbHelper.getWritableDatabase();
    this.parser = parser;
  }

  @Nullable public Record loadRecord(String key) {
    return selectRecordForKey(key).orNull();
  }

  @Nonnull public Set<String> merge(Record apolloRecord) {
    Optional<Record> optionalOldRecord = selectRecordForKey(apolloRecord.key());
    Set<String> changedKeys;
    if (!optionalOldRecord.isPresent()) {
      createRecord(apolloRecord.key(), parser.toJson(apolloRecord.fields()));
      changedKeys = Collections.emptySet();
    } else {
      Record oldRecord = optionalOldRecord.get();
      changedKeys = oldRecord.mergeWith(apolloRecord);
      if (!changedKeys.isEmpty()) {
        updateRecord(oldRecord.key(), parser.toJson(oldRecord.fields()));
      }
    }
    return changedKeys;
  }

  @Nonnull @Override public Set<String> merge(Collection<Record> recordSet) {
    Set<String> changedKeys = Collections.emptySet();
    try {
      database.beginTransaction();
      changedKeys = super.merge(recordSet);
      database.setTransactionSuccessful();
    } finally {
      database.endTransaction();
    }
    return changedKeys;
  }

  long createRecord(String key, String fields) {
    ContentValues values = contentValuesForRecord(key, fields);
    long recordId = database.insert(TABLE_RECORDS, null, values);
    return recordId;
  }

  void updateRecord(String key, String fields) {
    ContentValues values = contentValuesForRecord(key, fields);
    String selection = COLUMN_KEY + " = ?";
    String[] selectionArgs = new String[]{key};
    database.update(TABLE_RECORDS, values, selection, selectionArgs);
  }

  private ContentValues contentValuesForRecord(String key, String fields) {
    ContentValues values = new ContentValues(2);
    values.put(COLUMN_KEY, key);
    values.put(COLUMN_RECORD, fields);
    return values;
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

  void deleteRecord(String key) {
    System.out.println("Record deleted with key: " + key);
    database.delete(ApolloSqlHelper.TABLE_RECORDS, ApolloSqlHelper.COLUMN_ID
        + " = ?", new String[]{key});
  }

  Record cursorToRecord(Cursor cursor) throws IOException {
    String key = cursor.getString(1);
    String jsonOfFields = cursor.getString(2);
    return new Record(key, parser.from(jsonOfFields));
  }

  public void close() {
    dbHelper.close();
  }
}
