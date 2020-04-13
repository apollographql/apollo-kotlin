package com.apollographql.apollo.cache.normalized.sql

import android.content.Context
import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

class SqlNormalizedCacheFactory internal constructor(
    driver: SqlDriver
) : NormalizedCacheFactory<SqlNormalizedCache>() {

  @JvmOverloads
  constructor(context: Context, name: String? = "apollo.db")
      : this(AndroidSqliteDriver(ApolloDatabase.Schema, context.applicationContext, name))

  constructor(apolloSqlHelper: ApolloSqlHelper) : this(apolloSqlHelper.sqlDriver)

  private val apolloDatabase = ApolloDatabase(driver)

  override fun create(recordFieldAdapter: RecordFieldJsonAdapter) =
      SqlNormalizedCache(recordFieldAdapter, apolloDatabase.cacheQueries)

}
