package com.apollographql.apollo.cache.normalized.sql.internal

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import com.apollographql.apollo.cache.normalized.sql.internal.json.JsonDatabase
import com.apollographql.apollo.exception.apolloExceptionHandler
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.db.use

internal fun createRecordDatabase(driver: SqlDriver): RecordDatabase {
  maybeCreateOrMigrateSchema(driver, getSchema())

  val tableNames = mutableListOf<String>()

  try {
    // https://sqlite.org/forum/info/d90adfbb0a6eea88
    // The name is sqlite_schema these days but older versions use sqlite_master and sqlite_master is recognized everywhere so use that
    driver.executeQuery(
        null,
        "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;",
        { cursor ->
          while (cursor.next().value) {
            tableNames.add(cursor.getString(0) ?: "")
          }
          QueryResult.Unit
        },
        0
    )
  } catch (e: Exception) {
    apolloExceptionHandler(Exception("An exception occurred while looking up the table names", e))
    /**
     * Best effort: if we can't find any table, open the DB anyway and let's see what's happening
     */
  }

  val expectedTableName = "records"

  check(tableNames.isEmpty() || tableNames.contains(expectedTableName)) {
    "Apollo: Cannot find the '$expectedTableName' table? (found '$tableNames' instead)"
  }

  return JsonRecordDatabase(JsonDatabase(driver).jsonQueries)
}

internal fun getSchema(): SqlSchema<QueryResult.Value<Unit>> = JsonDatabase.Schema
