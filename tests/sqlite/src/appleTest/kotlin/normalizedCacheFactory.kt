package test

import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory

actual fun normalizedCacheFactory(): SqlNormalizedCacheFactory {
  return SqlNormalizedCacheFactory("apollo", true)
}