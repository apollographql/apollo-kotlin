package com.apollographql.android.cache.normalized.sql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.apollographql.android.api.graphql.internal.Optional;
import com.apollographql.android.cache.normalized.CacheStore;
import com.apollographql.android.cache.normalized.FieldsAdapter;
import com.apollographql.android.cache.normalized.Record;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.COLUMN_KEY;
import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.COLUMN_RECORD;
import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.TABLE_RECORDS;

public final class SqlStore extends CacheStore {
  private static final String INSERT_STATEMENT =
      String.format("INSERT INTO %s (%s,%s) VALUES (?,?)",
      TABLE_RECORDS,
      COLUMN_KEY,
      COLUMN_RECORD);
  SQLiteDatabase database;
  private final FieldsAdapter parser;
  private final ApolloSqlHelper dbHelper;
  private final String[] allColumns = {ApolloSqlHelper.COLUMN_ID,
      ApolloSqlHelper.COLUMN_KEY,
      ApolloSqlHelper.COLUMN_RECORD};
  private final SQLiteStatement sqLiteStatement;


  public static SqlStore create(ApolloSqlHelper helper, FieldsAdapter adapter) {
    return new SqlStore(helper, adapter);
  }

  private SqlStore(ApolloSqlHelper dbHelper, FieldsAdapter parser) {
    this.dbHelper = dbHelper;
    database = dbHelper.getWritableDatabase();
    this.parser = parser;
    sqLiteStatement = database.compileStatement(INSERT_STATEMENT);
  }

  @Nullable public Record loadRecord(String key) {
    return selectRecordForKey(key).get();
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
    sqLiteStatement.bindString(1, key);
    sqLiteStatement.bindString(2, fields);

    long recordId = sqLiteStatement.executeInsert();
    return recordId;
  }

  void updateRecord(String key, String fields) {
    sqLiteStatement.bindString(1, key);
    sqLiteStatement.bindString(2, fields);
    sqLiteStatement.executeUpdateDelete();
  }

  Optional<Record> selectRecordForKey(String key) {
    Cursor cursor = database.query(TABLE_RECORDS,
        allColumns, ApolloSqlHelper.COLUMN_KEY + " = " + key, null,
        null, null, null);
    cursor.moveToFirst();
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
        + " = " + key, null);
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
