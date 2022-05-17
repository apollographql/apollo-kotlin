package com.apollographql.apollo3.cache.normalized.sql.internal
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.internal.blob.BlobDatabase
import com.apollographql.apollo3.cache.internal.json.JsonDatabase
import com.apollographql.apollo3.exception.apolloExceptionHandler
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.db.use

@OptIn(ApolloExperimental::class)
internal fun createRecordDatabase(driver: SqlDriver, withDates: Boolean): RecordDatabase {
  val tableName = try {
    driver.executeQuery(null, "SELECT name FROM sqlite_schema WHERE type='table' ORDER BY name;", 0).use {
      it.getString(0)
    }
  } catch (e: Exception) {
    apolloExceptionHandler(Exception("An exception occurred while looking up the table names", e))
    null
  }
  val expectedTableName = if (withDates) "blobs" else "records"

  check (tableName == null || tableName == expectedTableName) {
    "Apollo: Cannot find the '$expectedTableName' table, did you change the 'withDates' parameter?"
  }

  createOrMigrateSchema(driver, getSchema(withDates))

  return if (withDates) {
    BlobRecordDatabase(BlobDatabase(driver).blobQueries)
  } else {
    JsonRecordDatabase(JsonDatabase(driver).jsonQueries)
  }
}

internal fun getSchema(withDates: Boolean): SqlDriver.Schema = if (withDates) {
  BlobDatabase.Schema
} else {
  JsonDatabase.Schema
}
