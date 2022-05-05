package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.internal.blob.BlobDatabase
import com.apollographql.apollo3.cache.internal.json.JsonDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.BlobRecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.JsonRecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.RecordDatabase
import com.squareup.sqldelight.db.SqlDriver

expect class SqlNormalizedCacheFactory internal constructor(
    driver: SqlDriver,
    withAge: Boolean = false,
) : NormalizedCacheFactory

internal fun createRecordDatabase(driver: SqlDriver, withAge: Boolean): RecordDatabase {
  return if (withAge) {
    BlobRecordDatabase(BlobDatabase(driver).blobQueries)
  } else {
    JsonRecordDatabase(JsonDatabase(driver).jsonQueries)
  }
}

internal fun getSchema(withAge: Boolean): SqlDriver.Schema {
  return if (withAge) {
    BlobDatabase.Schema
  } else {
    JsonDatabase.Schema
  }
}


expect fun createSqlNormalizedCacheFactory(name: String, withAge: Boolean): SqlNormalizedCacheFactory