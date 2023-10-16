package com.apollographql.ijplugin.normalizedcache

import com.apollographql.ijplugin.normalizedcache.NormalizedCache.Field
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.BooleanValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.CompositeValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.ListValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Null
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.NumberValue
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.Reference
import com.apollographql.ijplugin.normalizedcache.NormalizedCache.FieldValue.StringValue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.sql.DriverManager

private const val REFERENCE_PREFIX = "ApolloCacheReference{"

class DatabaseNormalizedCacheProvider : NormalizedCacheProvider<File> {
  override fun provide(parameters: File): Result<NormalizedCache> {
    Class.forName("org.sqlite.JDBC")
    return runCatching {
      DriverManager.getConnection("jdbc:sqlite:${parameters.absolutePath}").use { connection ->
        val resultSet = connection.createStatement().executeQuery("SELECT key, record FROM records")
        val records = mutableListOf<NormalizedCache.Record>()
        while (resultSet.next()) {
          val key = resultSet.getString(1)
          val recordJsonStr = resultSet.getString(2)
          val recordJson = Json.parseToJsonElement(recordJsonStr).jsonObject
          records.add(NormalizedCache.Record(key, recordJson.map {
            Field(it.key, it.value.toFieldValue())
          }))
        }
        NormalizedCache(records)
      }
    }
  }
}

private fun JsonElement.toFieldValue(): FieldValue {
  return when (this) {
    is JsonNull -> Null
    is JsonPrimitive -> {
      when {
        isString -> if (content.startsWith(REFERENCE_PREFIX)) {
          Reference(content.substringAfter(REFERENCE_PREFIX).substringBeforeLast('}'))
        } else {
          StringValue(content)
        }

        content == "true" || content == "false" -> BooleanValue(content.toBooleanStrict())
        else -> NumberValue(content.toIntOrNull() ?: content.toDoubleOrNull()
        ?: error("Could not parse number '$content'"))
      }
    }

    is JsonArray -> ListValue(map { it.toFieldValue() })
    is JsonObject -> CompositeValue(map { Field(it.key, it.value.toFieldValue()) })
  }
}
