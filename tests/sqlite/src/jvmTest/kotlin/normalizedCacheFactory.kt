package test

import com.apollographql.apollo3.cache.normalized.sql.SqlNormalizedCacheFactory
import java.util.Properties

actual fun normalizedCacheFactory(): SqlNormalizedCacheFactory {
   return SqlNormalizedCacheFactory("jdbc:sqlite:", Properties(), true)
}