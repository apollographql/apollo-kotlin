package com.apollographql.apollo.cache.normalized.sql

import android.content.Context
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

object ApolloSqlHelper {

  @JvmOverloads
  @JvmStatic
  fun create(context: Context, name: String? = null): SqlDriver {
    return AndroidSqliteDriver(ApolloDatabase.Schema, context.applicationContext, name)
  }
}
