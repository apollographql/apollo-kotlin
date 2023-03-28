package com.apollographql.apollo3.cache.normalized.sql

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.sql.internal.createDriver
import com.apollographql.apollo3.cache.normalized.sql.internal.createRecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.getSchema
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

actual class SqlNormalizedCacheFactory actual constructor(
    private val driver: SqlDriver,
    private val withDates: Boolean,
) : NormalizedCacheFactory() {

  /**
   * @param [name] Name of the database file, or null for an in-memory database (as per Android framework implementation).
   * @param [factory] Factory class to create instances of [SupportSQLiteOpenHelper]
   * @param [useNoBackupDirectory] Sets whether to use a no backup directory or not.
   */
  @JvmOverloads
  constructor(
      context: Context,
      name: String? = "apollo.db",
      factory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
      useNoBackupDirectory: Boolean = false,
      withDates: Boolean = false,
  ) : this(
      AndroidSqliteDriver(
          getSchema(withDates),
          context.applicationContext,
          name,
          factory,
          useNoBackupDirectory = useNoBackupDirectory
      ),
      withDates
  )

  actual constructor(name: String?, withDates: Boolean): this(createDriver(name, null, getSchema(withDates)), withDates)

  override fun create(): SqlNormalizedCache {
    return SqlNormalizedCache(createRecordDatabase(driver, withDates))
  }
}

