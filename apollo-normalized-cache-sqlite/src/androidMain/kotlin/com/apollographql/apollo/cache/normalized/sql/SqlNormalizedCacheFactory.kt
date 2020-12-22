package com.apollographql.apollo.cache.normalized.sql

import android.content.Context
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.apollographql.apollo.cache.normalized.sql.apollonormalizedcachesqlite.newInstance
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

actual class SqlNormalizedCacheFactory internal actual constructor(
    driver: SqlDriver
) : NormalizedCacheFactory<SqlNormalizedCache>() {

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
      useNoBackupDirectory: Boolean = false
  ) : this(
      AndroidSqliteDriver(
          ApolloDatabase.Schema, context.applicationContext, name, factory, useNoBackupDirectory = useNoBackupDirectory
      )
  )

  private val apolloDatabase = ApolloDatabase::class.newInstance(driver)

  override fun create(recordFieldAdapter: RecordFieldJsonAdapter) =
      SqlNormalizedCache(recordFieldAdapter, apolloDatabase, apolloDatabase.cacheQueries)

}
