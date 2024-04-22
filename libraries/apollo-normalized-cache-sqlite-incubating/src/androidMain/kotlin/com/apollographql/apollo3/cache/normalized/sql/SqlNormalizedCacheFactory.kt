package com.apollographql.apollo3.cache.normalized.sql

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.apollographql.apollo3.cache.normalized.api.NormalizedCacheFactory
import com.apollographql.apollo3.cache.normalized.sql.internal.createDriver
import com.apollographql.apollo3.cache.normalized.sql.internal.createRecordDatabase
import com.apollographql.apollo3.cache.normalized.sql.internal.getSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cash.sqldelight.db.SqlDriver
import com.apollographql.apollo3.cache.normalized.api.NormalizedCache

actual class SqlNormalizedCacheFactory actual constructor(
    private val driver: SqlDriver,
    private val withDates: Boolean,
) : NormalizedCacheFactory() {

  /**
   * @param [name] Name of the database file, or null for an in-memory database (as per Android framework implementation).
   * @param [factory] Factory class to create instances of [SupportSQLiteOpenHelper]
   * @param [configure] Optional callback, called when the database connection is being configured, to configure features such as
   *                    write-ahead logging or foreign key support. It should not modify the database except to configure it.
   * @param [useNoBackupDirectory] Sets whether to use a no backup directory or not.
   * @param [windowSizeBytes] Size of cursor window in bytes, per [android.database.CursorWindow] (Android 28+ only), or null to use the default.
   */
  @JvmOverloads
  constructor(
      context: Context,
      name: String? = "apollo.db",
      factory: SupportSQLiteOpenHelper.Factory = FrameworkSQLiteOpenHelperFactory(),
      configure: ((SupportSQLiteDatabase) -> Unit)? = null,
      useNoBackupDirectory: Boolean = false,
      windowSizeBytes: Long? = null,
      withDates: Boolean = false,
  ) : this(
      AndroidSqliteDriver(
          getSchema(withDates),
          context.applicationContext,
          name,
          factory,
          object : AndroidSqliteDriver.Callback(getSchema(withDates)) {
            override fun onConfigure(db: SupportSQLiteDatabase) {
              super.onConfigure(db)
              db.enableWriteAheadLogging()
              configure?.invoke(db)
            }
          },
          useNoBackupDirectory = useNoBackupDirectory,
          windowSizeBytes = windowSizeBytes,
      ),
      withDates
  )

  actual constructor(name: String?, withDates: Boolean): this(createDriver(name, null, getSchema(withDates)), withDates)

  actual override fun create(): NormalizedCache {
    return SqlNormalizedCache(createRecordDatabase(driver, withDates))
  }
}

