package com.apollographql.ijplugin.normalizedcache.provider

import com.apollographql.apollo.api.Error
import com.apollographql.apollo.api.json.JsonNumber
import com.apollographql.cache.normalized.api.CacheKey
import com.apollographql.ijplugin.normalizedcache.NormalizedCache
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.Field
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.BooleanValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.CompositeValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.ErrorValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.ListValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Null
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.NumberValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Reference
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.StringValue
import java.io.File
import java.sql.DriverManager
import com.apollographql.apollo.cache.normalized.api.CacheKey as ApolloClassicCacheKey
import com.apollographql.apollo.cache.normalized.api.NormalizedCache as ApolloClassicNormalizedCache
import com.apollographql.apollo.cache.normalized.api.Record as ApolloClassicRecord
import com.apollographql.apollo.cache.normalized.sql.SqlNormalizedCacheFactory as ApolloClassicSqlNormalizedCacheFactory
import com.apollographql.cache.normalized.api.CacheKey as ApolloModernCacheKey
import com.apollographql.cache.normalized.api.NormalizedCache as ApolloModernNormalizedCache
import com.apollographql.cache.normalized.api.Record as ApolloModernRecord
import com.apollographql.cache.normalized.sql.SqlNormalizedCacheFactory as ApolloModernSqlNormalizedCacheFactory

class DatabaseNormalizedCacheProvider : NormalizedCacheProvider<File> {
  private enum class DbFormat {
    /**
     * Classic format with records containing JSON text, implemented in
     * `com.apollographql.apollo:apollo-normalized-cache-sqlite`.
     */
    CLASSIC,

    /**
     * Modern format with records containing a binary format, implemented in
     * `com.apollographql.cache:normalized-cache-sqlite-incubating`.
     */
    MODERN,

    UNKNOWN_OR_ERROR,
  }

  private fun checkDatabase(url: String): DbFormat {
    Class.forName("org.sqlite.JDBC")

    DriverManager.getConnection(url).use { connection ->
      runCatching {
        connection.createStatement().executeQuery("SELECT key, record FROM records").use { resultSet ->
          if (resultSet.next()) {
            return DbFormat.CLASSIC
          }
        }
      }
      runCatching {
        connection.createStatement().executeQuery("SELECT key, record FROM record").use { resultSet ->
          if (resultSet.next()) {
            return DbFormat.MODERN
          }
        }
      }
    }
    return DbFormat.UNKNOWN_OR_ERROR
  }

  override fun provide(parameters: File): Result<NormalizedCache> {
    val url = "jdbc:sqlite:${parameters.absolutePath}"
    return runCatching {
      val format = checkDatabase(url)
      when (format) {
        DbFormat.CLASSIC -> readClassicDb(url)
        DbFormat.MODERN -> readModernDb(url)
        DbFormat.UNKNOWN_OR_ERROR -> error("Empty cache: no records were found")
      }
    }
  }

  private fun readClassicDb(url: String): NormalizedCache {
    val apolloNormalizedCache: ApolloClassicNormalizedCache = ApolloClassicSqlNormalizedCacheFactory(url).create()
    val apolloRecords: Map<String, ApolloClassicRecord> = apolloNormalizedCache.dump().values.first()
    return NormalizedCache(
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

  private fun readModernDb(url: String): NormalizedCache {
    val apolloNormalizedCache: ApolloModernNormalizedCache = ApolloModernSqlNormalizedCacheFactory(url).create()
    val apolloRecords: Map<CacheKey, ApolloModernRecord> = apolloNormalizedCache.dump().values.first()
    return NormalizedCache(
        apolloRecords.map { (key, apolloRecord) ->
          NormalizedCache.Record(
              key = key.key,
              fields = apolloRecord.map { (fieldKey, fieldValue) ->
                Field(fieldKey, fieldValue.toFieldValue())
              },
              sizeInBytes = apolloRecord.sizeInBytes
          )
        }
    )
  }
}

private fun Any?.toFieldValue(): FieldValue {
  return when (this) {
    null -> Null
    is String -> StringValue(this)
    is Number -> NumberValue(this.toString())
    is JsonNumber -> NumberValue(this.value)
    is Boolean -> BooleanValue(this)
    is List<*> -> ListValue(map { it.toFieldValue() })
    is Map<*, *> -> CompositeValue(map { Field(it.key as String, it.value.toFieldValue()) })
    is ApolloClassicCacheKey -> Reference(this.key)
    is ApolloModernCacheKey -> Reference(this.key)
    is Error -> ErrorValue(this.message)
    else -> error("Unsupported type ${this::class}")
  }
}
