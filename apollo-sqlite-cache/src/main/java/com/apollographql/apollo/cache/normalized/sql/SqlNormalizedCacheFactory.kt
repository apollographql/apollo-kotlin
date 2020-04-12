package com.apollographql.apollo.cache.normalized.sql

import com.apollographql.apollo.cache.normalized.NormalizedCacheFactory
import com.apollographql.apollo.cache.normalized.RecordFieldJsonAdapter
import com.squareup.sqldelight.db.SqlDriver

class SqlNormalizedCacheFactory(
    driver: SqlDriver
) : NormalizedCacheFactory<SqlNormalizedCache>() {

  private val apolloDatabase = ApolloDatabase(driver)

  override fun create(recordFieldAdapter: RecordFieldJsonAdapter) =
      SqlNormalizedCache(recordFieldAdapter, apolloDatabase.cacheQueries)

}
