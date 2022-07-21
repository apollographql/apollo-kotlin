package com.apollographql.apollo3.cache.normalized.sql

import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.cache.normalized.sql.internal.blob2.Blob2Database
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.sql.internal.Blob2RecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.maybeCreateOrMigrateSchema
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import java.io.File

/**
 * Experimental database that supports trimming at startup
 *
 * There are no backward compatibilities, DO NOT ship in a production app
 *
 * @param url Database connection URL in the form of `jdbc:sqlite:path` where `path` is either blank
 * @param maxSize if the size of the database is bigger than [maxSize] (in bytes), it will be trimmed
 * @param trimFactor the amount of trimming to do
 */
@ApolloExperimental
class TrimmableNormalizedCacheFactory internal constructor(
    private val url: String,
    private val maxSize: Long? = null,
    private val trimFactor: Float = 0.1f,
) : NormalizedCacheFactory() {
  private val driver = JdbcSqliteDriver(url)

  override fun create(): SqlNormalizedCache {
    maybeCreateOrMigrateSchema(driver, Blob2Database.Schema)

    val database = Blob2Database(driver)
    val queries = database.blob2Queries
    if (maxSize != null) {
      val path = url.substringAfter("jdbc:sqlite:")
      if (path.isNotBlank()) {
        val size = File(path).length()
        if (size >= maxSize) {
          val count = queries.count().executeAsOne()
          queries.trim((count * trimFactor).toLong())
          driver.execute(null, "VACUUM", 0)
        }
      }
    }

    return SqlNormalizedCache(Blob2RecordDatabase(queries))
  }
}


