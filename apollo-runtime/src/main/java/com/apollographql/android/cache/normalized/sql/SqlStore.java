package com.apollographql.android.cache.normalized.sql;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.apollographql.android.cache.normalized.CacheStore;
import com.apollographql.android.cache.normalized.Record;

import java.sql.SQLException;

import javax.annotation.Nullable;

import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.COLUMN_KEY;
import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.COLUMN_RECORD;
import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.TABLE_RECORDS;

final class SqlStore extends CacheStore {

  // Database fields
  SQLiteDatabase database; //exposed for testing Mike will fix if a better path can be found.
  private final ApolloSqlHelper dbHelper;
  private final String[] allColumns = {ApolloSqlHelper.COLUMN_ID,
      ApolloSqlHelper.COLUMN_KEY,
      ApolloSqlHelper.COLUMN_RECORD};
  private final SQLiteStatement sqLiteStatement;
  private static final String insertStatement = String.format("INSERT INTO+%s(%s,%s) VALUES (?,?)",
      TABLE_RECORDS,
      COLUMN_KEY,
      COLUMN_RECORD);

  public static SqlStore create(ApolloSqlHelper helper) {
    return new SqlStore(helper);
  }

  private SqlStore(ApolloSqlHelper dbHelper) {
    this.dbHelper = dbHelper;
    sqLiteStatement = database.compileStatement(insertStatement);
  }

  @Nullable @Override public Record loadRecord(String key) {
    return null;
  }

  @Override public void merge(Record object) {

  }

  public Record createRecord(String key, String record) {
    sqLiteStatement.bindString(0, key);
    sqLiteStatement.bindString(0, record);

    long recordId = sqLiteStatement.executeInsert();

    Cursor cursor = database.query(TABLE_RECORDS,
        allColumns, ApolloSqlHelper.COLUMN_ID + " = " + recordId, null,
        null, null, null);
    cursor.moveToFirst();
    Record newRecord = cursorToRecord(cursor);
    cursor.close();
    return newRecord;
  }

  public void deleteRecord(String key) {
    System.out.println("Record deleted with key: " + key);
    database.delete(ApolloSqlHelper.TABLE_RECORDS, ApolloSqlHelper.COLUMN_ID
        + " = " + key, null);
  }

  //TODO wire to RecordParser
  private String toJson(Record record) {
    return null;
  }

  //TODO wire to RecordParser
  private Record cursorToRecord(Cursor cursor) {
    Record record = new Record(cursor.getString(1));
    throw new RuntimeException();
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    dbHelper.close();
  }

}
