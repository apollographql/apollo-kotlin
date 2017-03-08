package com.apollographql.android.cache.normalized.sql;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.apollographql.android.cache.normalized.Record;

import static com.apollographql.android.cache.normalized.sql.MySQLiteHelper.COLUMN_KEY;
import static com.apollographql.android.cache.normalized.sql.MySQLiteHelper.COLUMN_RECORD;
import static com.apollographql.android.cache.normalized.sql.MySQLiteHelper.TABLE_RECORDS;

public class RecordPersister {

  // Database fields
  private SQLiteDatabase database;
  private MySQLiteHelper dbHelper;
  private String[] allColumns = {MySQLiteHelper.COLUMN_ID,
      MySQLiteHelper.COLUMN_RECORD};
  private SQLiteStatement sqLiteStatement;
  private String insertStatement;

  public RecordPersister(Context context) {
    dbHelper = new MySQLiteHelper(context);
    insertStatement = String.format("INSERT INTO+%s(%s,%s) VALUES (?,?)",
        TABLE_RECORDS,
        COLUMN_KEY,
        COLUMN_RECORD);
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    dbHelper.close();
  }

  public Record createRecord(Record record) {
    ;
    sqLiteStatement = database.compileStatement(insertStatement);
    sqLiteStatement.bindString(0, record.key());
    sqLiteStatement.bindString(0, toJson(record));

    long recordId = sqLiteStatement.executeInsert();

    Cursor cursor = database.query(TABLE_RECORDS,
        allColumns, MySQLiteHelper.COLUMN_ID + " = " + recordId, null,
        null, null, null);
    cursor.moveToFirst();
    Record newRecord = cursorToRecord(cursor);
    cursor.close();
    return newRecord;
  }

  public void deleteRecord(String key) {
    System.out.println("Record deleted with key: " + key);
    database.delete(MySQLiteHelper.TABLE_RECORDS, MySQLiteHelper.COLUMN_ID
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
}
