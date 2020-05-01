package com.apollographql.apollo.cache.normalized.sql

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

@Deprecated("Use SqlNormalizedCacheFactory constructor instead")
class ApolloSqlHelper private constructor(internal val sqlDriver: SqlDriver) {

  constructor(context: Context, name: String?)
      : this(AndroidSqliteDriver(ApolloDatabase.Schema, context.applicationContext, name))

  companion object {
    @JvmOverloads
    @JvmStatic
    @Deprecated(
        message = "Use SqlNormalizedCacheFactory constructor instead",
        replaceWith = ReplaceWith("SqlNormalizedCacheFactory(context, name)"),
        level = DeprecationLevel.ERROR
    )
    fun create(context: Context, name: String? = "apollo.db"): ApolloSqlHelper {
      return ApolloSqlHelper(context, name)
    }
  }
}
