package com.apollographql.ijplugin.normalizedcache.provider

import com.apollographql.apollo.cache.normalized.api.CacheKey
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory
import com.apollographql.ijplugin.normalizedcache.NormalizedCache
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.Field
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.BooleanValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.CompositeValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.ListValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Null
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.NumberValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Reference
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.StringValue
import java.io.File
import java.sql.DriverManager
import com.apollographql.apollo.cache.normalized.api.NormalizedCache as ApolloNormalizedCache
import com.apollographql.apollo.cache.normalized.api.Record as ApolloRecord

class DatabaseNormalizedCacheProvider : NormalizedCacheProvider<File> {
  private fun checkDatabase(url: String) {
    Class.forName("org.sqlite.JDBC")
    DriverManager.getConnection(url).use { connection ->
      connection.createStatement().executeQuery("SELECT key, record FROM records").use { resultSet ->
        if (!resultSet.next()) {
          error("Empty cache: no records were found")
        }
      }
    }
  }

  override fun provide(parameters: File): Result<NormalizedCache> {
    val url = "jdbc:sqlite:${parameters.absolutePath}"
    return runCatching {
      checkDatabase(url)
      val apolloNormalizedCache: ApolloNormalizedCache = SqlNormalizedCacheFactory(url).create()
      val apolloRecords: Map<String, ApolloRecord> = apolloNormalizedCache.dump().values.first()
      NormalizedCache(
          apolloRecords.map { (key, apolloRecord) ->
            NormalizedCache.Record(
                key = key,
                fields = apolloRecord.map { (fieldKey, fieldValue) ->
                  Field(fieldKey, fieldValue.toFieldValue())
                },
                sizeInBytes = apolloRecord.sizeInBytes
            )
          }
      )
    }
  }
}

private fun Any?.toFieldValue(): FieldValue {
  return when (this) {
    null -> Null
    is String -> StringValue(this)
    is Number -> NumberValue(this)
    is Boolean -> BooleanValue(this)
    is List<*> -> ListValue(map { it.toFieldValue() })
    is Map<*, *> -> CompositeValue(map { Field(it.key as String, it.value.toFieldValue()) })
    is CacheKey -> Reference(this.key)
    else -> error("Unsupported type ${this::class}")
  }
}
