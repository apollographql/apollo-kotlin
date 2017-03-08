package com.apollographql.android.cache.normalized.sql;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.apollographql.android.cache.normalized.CacheStore;
import com.apollographql.android.cache.normalized.Record;

import javax.annotation.Nullable;

import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.COLUMN_KEY;
import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.COLUMN_RECORD;
import static com.apollographql.android.cache.normalized.sql.ApolloSqlHelper.TABLE_RECORDS;

final class SqlStore extends CacheStore {

  // Database fields
  private SQLiteDatabase database;
  private ApolloSqlHelper dbHelper;
  private String[] allColumns = {ApolloSqlHelper.COLUMN_ID, ApolloSqlHelper.COLUMN_KEY, ApolloSqlHelper.COLUMN_RECORD};
  private SQLiteStatement sqLiteStatement;
  private String insertStatement;

  public static SqlStore create(ApolloSqlHelper helper) {
    return new SqlStore(helper);
  }

  private SqlStore(ApolloSqlHelper dbHelper) {
    this.dbHelper = dbHelper;
    insertStatement = String.format("INSERT INTO+%s(%s,%s) VALUES (?,?)",
        TABLE_RECORDS,
        COLUMN_KEY,
        COLUMN_RECORD);
  }


  @Nullable @Override public Record loadRecord(String key) {
    return null;
  }

  @Override public void merge(Record object) {

  }


  public Record createRecord(String key, String record) {
    sqLiteStatement = database.compileStatement(insertStatement);
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
//    Record record = new Record();
//    record.setId(cursor.getLong(0));
//    record.setRecord(cursor.getString(1));
    throw new RuntimeException();
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    dbHelper.close();
  }

}
