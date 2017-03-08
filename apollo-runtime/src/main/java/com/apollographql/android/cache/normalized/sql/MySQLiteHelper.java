package com.apollographql.android.cache.normalized.sql;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MySQLiteHelper extends SQLiteOpenHelper {

  public static final String TABLE_RECORDS = "records";
  public static final String COLUMN_ID = "_id";
  public static final String COLUMN_RECORD = "record";
  public static final String COLUMN_KEY = "key";

  private static final String DATABASE_NAME = "commments.db";
  private static final int DATABASE_VERSION = 1;

  // Database creation sql statement
  private static final String DATABASE_CREATE = String.format(
      "create table %s( %s integer primary key autoincrement, " +
          "%s text not null," +
          "%s text not null);", TABLE_RECORDS, COLUMN_ID, COLUMN_RECORD, COLUMN_KEY);

  public MySQLiteHelper(Context context) {
    super(context, DATABASE_NAME, null, DATABASE_VERSION);
  }

  @Override
  public void onCreate(SQLiteDatabase database) {
    database.execSQL(DATABASE_CREATE);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    Log.w(MySQLiteHelper.class.getName(),
        "Upgrading database from version " + oldVersion + " to "
            + newVersion + ", which will destroy all old data");
    db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECORDS);
    onCreate(db);
  }

}
